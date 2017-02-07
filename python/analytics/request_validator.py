import datetime


def validate(request):
    request_stamp = request.args.get('timestamp')
    if not validate_timestamp(request_stamp):
        return False
    if request.method == 'POST':
        if not validate_post(request.args):
            return False
    elif request.method == 'GET':
        return True
    else:
        return False
    return True


def validate_timestamp(request_stamp):
    try:
        if request_stamp is None:
            return False

        request_timestamp = datetime.datetime.fromtimestamp(int(request_stamp) / 1000)
        current_time = datetime.datetime.now()
        if request_timestamp.year < 2000:
            return False

        if current_time < request_timestamp:
            return False
        return True
    except Exception:
        return False


def validate_post(request_args):
    username = request_args.get('user')
    if username is None or not username.isalnum():
        return False
    if len(username) > 20:
        return False
    if "click" not in request_args and "impression" not in request_args:
        return False
    return True
