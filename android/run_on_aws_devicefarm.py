import argparse
import datetime
import multiprocessing
import os
import requests
import time
import logging
import json
import subprocess

import boto3

# setup logger
logging.basicConfig(
    format='%(asctime)s %(levelname)-6s %(name)-20s %(message)s',
    level=logging.INFO
)
logger = logging.getLogger('main')

# get with `aws devicefarm list-projects` (make sure to set region to us-west-2,
# as it is the only one with device farm)
PROJECT_ARN = 'CHANGE_ME'

# get with `aws devicefarm list-device-pools --arn $PROJECT_ARN`
DEVICE_POOL_ALIASES = {
    'single': 'CHANGE_ME',
    'small': 'CHANGE_ME',
}

ANDROID_PROJECT_PATH = '.'

APK_FILES = {
    'app': os.path.join(ANDROID_PROJECT_PATH, 'app/build/outputs/apk/debug/app-debug.apk'),
    'tests': os.path.join(ANDROID_PROJECT_PATH, 'bench/build/outputs/apk/androidTest/debug/bench-debug-androidTest.apk'),
}


def ensure_device_pool_static(device_pool_info):
    pool = device_pool_info['devicePool']
    rules = pool['rules']
    # we expect exactly one rule, the static device selection rule
    assert len(rules) == 1
    assert rules[0]['attribute'] == 'ARN' and rules[0]['operator'] == 'IN'
    devices = json.loads(rules[0]['value'])
    logger.info(
        f'Device pool found and verified. It has {len(devices)} devices.')


def run_apk_builds():
    subprocess.check_call(
        ['./gradlew', 'assembleDebug', 'assembleAndroidTest'],
        cwd=ANDROID_PROJECT_PATH,
    )
    logger.info('Android ./gradlew build successful')


def ensure_apks(rebuild_apk):
    if rebuild_apk:
        run_apk_builds()
    else:
        logger.info('Skipping APK builds (use --rebuild-apk to force build)')

    for name, file in APK_FILES.items():
        if os.path.exists(file):
            logger.info(f'Found {name} APK file: {file}')
        else:
            logger.warning(f'Missing {name} APK file: {file}')


def run(client, device_pool_alias, rebuild_apk):
    logger.info(f'{client=}')

    # get device pool information
    device_pool_arn=DEVICE_POOL_ALIASES[device_pool_alias]
    ensure_device_pool_static(client.get_device_pool(arn=device_pool_arn))

    # ensure APKs: fail if there are none and build if --rebuild-apks flag is set
    ensure_apks(rebuild_apk=rebuild_apk)

    # create new run identifier (for prefixes uploads)
    run_id = device_pool_alias + '-hw-support-' + datetime.datetime.now().strftime('%Y-%m-%d-%H%M%S')
    logger.info(f'{run_id=}')

    # upload artifacts
    app_upload_arn = df_upload_file(
        client, run_id, APK_FILES['app'], 'ANDROID_APP')
    logger.info(f'SUCCEEDED: {app_upload_arn=}')
    tests_upload_arn = df_upload_file(
        client, run_id, APK_FILES['tests'], 'INSTRUMENTATION_TEST_PACKAGE')
    logger.info(f'SUCCEEDED: {tests_upload_arn=}')

    # start run and wait for completion
    try:
        run_arn = df_schedule_run(
            client,
            run_id, device_pool_arn,
            app_upload_arn, tests_upload_arn
        )
        logger.info(f'Scheduled as {run_arn=}')
        df_wait_for_run_finished(client, run_arn)
    except:
        logger.warning(
            f'Something went wrong (or CTRL+C). Trying to stop {run_arn=}')
        client.stop_run(arn=run_arn)
        exit(1)

    df_download_and_store_results(client, run_id, run_arn)

#
# The `df_...` methods below are based on the example code provided by AWS:
# https://docs.aws.amazon.com/code-library/latest/ug/python_3_device-farm_code_examples.html
#


def df_schedule_run(client, run_id, device_pool_arn, app_upload_arn, test_upload_arn):
    response = client.schedule_run(
        projectArn=PROJECT_ARN,
        appArn=app_upload_arn,
        devicePoolArn=device_pool_arn,
        name=run_id,
        test={
            "type": "INSTRUMENTATION",
            "testPackageArn": test_upload_arn
        }
    )
    run_arn = response['run']['arn']
    return run_arn


def df_wait_for_run_finished(client, run_arn):
    while True:
        response = client.get_run(arn=run_arn)
        run_state = response['run']['status']
        if run_state == 'COMPLETED' or run_state == 'ERRORED':
            logger.info(f'Run reached state {run_state}')
            break
        else:
            logger.info(f'Run is in state {run_state}... waiting')
            time.sleep(10)


def df_upload_file(client, run_id, filename, typename, mime='application/octet-stream'):
    # register upload
    upload_response = client.create_upload(
        projectArn=PROJECT_ARN,
        name=f'{run_id}-{os.path.basename(filename)}',
        type=typename,
        contentType=mime
    )
    upload_arn = upload_response['upload']['arn']
    upload_url = upload_response['upload']['url']

    # perform upload
    with open(filename, 'rb') as file_stream:
        upload_name = upload_response['upload']['name']
        logger.info(f'Uploading {filename} to Device Farm as {upload_name}')
        put_req = requests.put(
            upload_url,
            data=file_stream,
            headers={'content-type': mime}
        )
        if not put_req.ok:
            raise Exception(f'Upload failed: {put_req.reason}')

    # await confirmation
    while True:
        upload_state = upload_response['upload']['status']

        if upload_state == 'INITIALIZED':
            pass
        elif upload_state == 'FAILED':
            upload_message = upload_response['upload']['message']
            raise Exception(f'The upload failed processing: {upload_message}')
        elif upload_state == 'SUCCEEDED':
            break
        else:
            logger.info(f'Upload of {filename} in state {upload_state}')

        time.sleep(3)
        upload_response = client.get_upload(arn=upload_arn)

    return upload_arn


def df_download_and_store_results(client, run_id, run_arn, results_dir='../results'):
    run_result_path = os.path.join(results_dir, run_id)
    os.makedirs(run_result_path, exist_ok=True)

    # We collect the download jobs for the individual tests (artifacts) first, before
    # then downloading them in parallel
    download_jobs = []

    jobs = client.list_jobs(arn=run_arn)['jobs']
    for i, job in enumerate(jobs):
        job_name = job['name'].replace(' ', '').replace('(', '').replace(')', '').replace('"', '')
        logger.info(f'Handling job {i+1}/{len(jobs)}: {job_name}')

        job_results_path = os.path.join(run_result_path, job_name)
        os.makedirs(job_results_path, exist_ok=True)

        # Save device information
        with open(os.path.join(job_results_path, 'device.json'), 'w') as f:
            json.dump(job['device'], f)

        # Each job will have suites that have tests that have artifacts of which some are logs
        suites = client.list_suites(arn=job['arn'])['suites']
        for suite in suites:
            for test in client.list_tests(arn=suite['arn'])['tests']:
                artifacts = client.list_artifacts(
                    type='LOG',
                    arn=test['arn']
                )['artifacts']
                for artifact in artifacts:
                    test_name = test['name'].replace(' ', '')
                    filename = f"{test_name}.{artifact['extension']}"
                    path = os.path.join(job_results_path, filename)

                    download_jobs.append((artifact['url'], path))

    # Download all artifacts in parallel
    worker_pool = multiprocessing.Pool()
    worker_pool.map(download, download_jobs)


def download(params):
    (from_url, to_path) = params
    with open(to_path, 'wb') as f:
        with requests.get(from_url, allow_redirects=True) as request:
            f.write(request.content)
            logger.info(f'-> stored: {to_path} ({len(request.content) // 1024} KiB)')


if __name__ == '__main__':
    parser = argparse.ArgumentParser('main')
    parser.add_argument('--device-pool', default='small')
    parser.add_argument('--rebuild-apks', action='store_true')

    args = parser.parse_args()

    run(
        client=boto3.client('devicefarm'),
        device_pool_alias=args.device_pool,
        rebuild_apk=args.rebuild_apks,
    )
