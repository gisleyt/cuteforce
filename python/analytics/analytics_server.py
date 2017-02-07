#!/usr/bin/env python3

import sys

import fakeredis
import redis
from flask import Flask, abort, request, current_app

import db.redis_helper
import request_validator as validator

app = Flask(__name__)
app.config.update(dict(
    redis_host='localhost',
    redis_port=6379
))


def get_redis():
    if not hasattr(current_app, 'redis'):
        current_app.redis = connect_redis()
    return current_app.redis


def connect_redis():
    if app.config['TESTING']:
        r = fakeredis.FakeStrictRedis()
        app.logger.warn(msg="Connected to fake server. NB: TESTING ONLY")
    else:
        r = redis.StrictRedis(host=app.config['redis_host'], port=app.config['redis_port'])
        app.logger.info(msg="Connected to redis server")
    return r


def get_hours_from_epoch(timestamp_ms):
    return int(timestamp_ms / (1000 * 60 * 60))


def persist(request_args):
    timestamp_ms = int(request_args.get('timestamp'))
    db.redis_helper.persist(get_redis(), get_hours_from_epoch(timestamp_ms), request_args)
    return 'ok', 200


def get_data(request_args):
    timestamp_ms = int(request_args.get('timestamp'))
    response_dict = db.redis_helper.get_data(get_redis(), get_hours_from_epoch(timestamp_ms))
    return "\n".join(["unqiue_users," + response_dict["unqiue_users"],
                      "clicks," + response_dict["clicks"],
                      "impressions," + response_dict["impressions"]
                      ])


@app.route('/analytics', methods=['GET', 'POST'])
def analytics():
    if not validator.validate(request):
        abort(400)
    if request.method == 'POST':
        return persist(request.args)
    else:
        return get_data(request.args)


if __name__ == '__main__':
    if "fakeredis" in sys.argv:
        app.config['TESTING'] = True
    app.run(debug=True)
