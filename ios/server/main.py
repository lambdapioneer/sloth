from flask import Flask
from flask import request

import datetime
import json
import os

app = Flask(__name__)


@app.route('/ios-report', methods = ['POST'])
def pde_se_ios():
    d = request.json
    print(d)

    normalized_name = d["device"].replace(',', '-').replace('/', '-').replace('.','-')
    normalized_version = d["version"].replace('.', '-')
    normalized_experiment = d["experiment"].replace('.', '-')
    filename = datetime.datetime.now().strftime('%Y-%m-%d-%H%M%S') + '_' + normalized_experiment + '_' + normalized_name + '_' + normalized_version + '.json'
    
    with open(os.path.join('data', filename), 'w') as f:
        json.dump(d, f, indent=4)
    
    return "OK"
