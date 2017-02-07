def get_data(r, hour_from_epoch):
    hour_from_epoch_str = str(hour_from_epoch)
    unique_users = r.scard("users-" + hour_from_epoch_str)
    clicks = r.get("click-" + hour_from_epoch_str)
    imp = r.get("imp-" + hour_from_epoch_str)
    if clicks is None:
        clicks = 0
    else:
        clicks = clicks.decode('utf-8')
    if imp is None:
        imp = 0
    else:
        imp = imp.decode('utf-8')

    response = {"unqiue_users": str(unique_users),
                "clicks": str(clicks),
                "impressions": str(imp)}
    return response


def persist(r, hour_from_epoch, request_param):
    hour_from_epoch_str = str(hour_from_epoch)
    if 'click' in request_param:
        r.incr("click-" + hour_from_epoch_str)
    else:
        r.incr("imp-" + hour_from_epoch_str)
    r.sadd("users-" + hour_from_epoch_str, request_param.get("user"))
