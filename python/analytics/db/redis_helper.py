def get_data(r, hour_from_epoch):
    unique_users = r.scard("users-" + str(hour_from_epoch))
    clicks = r.get("click-" + str(hour_from_epoch))
    imp = r.get("imp-" + str(hour_from_epoch))
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


def persist(r, hours_from_epoch, request_param):
    if 'click' in request_param:
        r.incr("click-" + str(hours_from_epoch))
    else:
        r.incr("imp-" + str(hours_from_epoch))
    r.sadd("users-" + str(hours_from_epoch), request_param.get("user"))
