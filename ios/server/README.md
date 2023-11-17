# Sloth: report server

This server is used by the iOS benchmark app to store the results of the individual runs.
We added this functionality to easily run many benchmarks on AWS DeviceFarm without having to manually copy the measurements.


## Running the server

This has been tested on Ubuntu 20.04.
First create a virtual environment and install all dependencies:

```bash
$ python3 -mvenv env
$ source env/bin/activate
(env) $ python3 -mpip install -r requirements.txt
```

Then start the server:

```bash
(env) $ python3 -mflask --app main run
```

The collected results will be available in the `data` folder.
