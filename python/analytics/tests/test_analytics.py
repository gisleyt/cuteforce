import unittest

import request_validator
import analytics_server
import fakeredis
import db.redis_helper as redis_helper
import datetime


class TestAnalyticsServer(unittest.TestCase):

    def setUp(self):
        self.app = analytics_server.app.config['TESTING'] = True
        self.app = analytics_server.app.test_client()

    def test_timestamp(self):
        assert(analytics_server.get_hours_from_epoch(1400000000000) == 388888)
        assert(analytics_server.get_hours_from_epoch(60000) == 0)
        assert (analytics_server.get_hours_from_epoch(3599000) == 0)
        assert (analytics_server.get_hours_from_epoch(3600000) == 1)
        assert (analytics_server.get_hours_from_epoch(7100000) == 1)
        assert (analytics_server.get_hours_from_epoch(7200000) == 2)

    def test_request_validation(self):

        param = dict(timestamp=1486209802000, user='foo', click='')
        assert(request_validator.validate_post(param))

        param['user'] = "waytolooooooooooooooooooooong"
        assert(not request_validator.validate_post(param))

        param['user'] = "justtherightlength13"
        assert(request_validator.validate_post(param))

        param['user'] = "invalidchars#"
        assert(not request_validator.validate_post(param))

        del param['click']
        assert(not request_validator.validate_post(param))

        assert(request_validator.validate_timestamp(datetime.datetime(2000, 1, 1).timestamp() * 1000))
        assert(not request_validator.validate_timestamp(datetime.datetime(1999, 1, 1).timestamp() * 1000))
        assert(not request_validator.validate_timestamp(datetime.datetime(2999, 1, 1).timestamp() * 1000))

    def test_persistence(self):
        r = fakeredis.FakeStrictRedis()
        for hour_from_epoch in range(10):
            for j in range(100):
                request_param = {}
                user = str(int(j / 5))
                request_param['user'] = "user" + user
                if j % 2 == 0:
                    request_param['click'] = ''
                else:
                    request_param['impression'] = ''

                redis_helper.persist(r, hour_from_epoch, request_param)

        for hour_from_epoch in range(10):
            response = redis_helper.get_data(r, hour_from_epoch)
            assert(response["impressions"] == '50')
            assert (response["clicks"] == '50')
            assert (response["unqiue_users"] == '20')

    def test_requests(self):
        for year in range(2000, 2015):
            for i in range(10):
                timestamp = int(datetime.datetime(year, 1, 1).timestamp() * 1000)
                if i < 3:
                    response = self.app.post("/analytics?timestamp=%s&user=%s&impression" % (str(timestamp), "user" + str(i)))
                    assert(response.status == '200 OK')
                else:
                    response = self.app.post("/analytics?timestamp=%s&user=%s&click" % (str(timestamp), "user" + str(i)))
                    assert (response.status == '200 OK')

        for year in range(2000, 2015):
            timestamp = int(datetime.datetime(year, 1, 1).timestamp() * 1000)
            res = self.app.get('/analytics?timestamp=%s' % str(timestamp))
            assert(res.status == '200 OK')
            assert(b'unqiue_users,10\nclicks,7\nimpressions,3' in res.data)

        timestamp = int(datetime.datetime(2010, 1, 1).timestamp() * 1000)
        res = self.app.put('/analytics?timestamp=%s' % str(timestamp))
        assert(res.status == '405 METHOD NOT ALLOWED')
        res = self.app.delete('/analytics?timestamp=%s' % str(timestamp))
        assert(res.status == '405 METHOD NOT ALLOWED')

        timestamp = int(datetime.datetime(1999, 1, 1).timestamp() * 1000)
        res = self.app.get('/analytics?timestamp=%s' % str(timestamp))
        assert (res.status == '400 BAD REQUEST')

        res = self.app.get('/analytics?timestamp=3498754395874876397543985')
        assert (res.status == '400 BAD REQUEST')

        res = self.app.get('/analytics?timestamp=3498')
        assert (res.status == '400 BAD REQUEST')
