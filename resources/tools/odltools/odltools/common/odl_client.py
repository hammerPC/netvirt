# Copyright 2018 Red Hat, Inc. and others. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
from requests import sessions

logger = logging.getLogger('common.odl_client')


class OpenDaylightRestClient(object):

    def __init__(self, url, username, password, timeout):
        super(OpenDaylightRestClient, self).__init__()
        self.url = url
        self.timeout = timeout
        self.session = sessions.Session()
        self.session.auth = (username, password)

    def get(self, urlpath='', data=None):
        return self.request('get', urlpath, data)

    def put(self, urlpath='', data=None):
        return self.request('put', urlpath, data)

    def delete(self, urlpath='', data=None):
        return self.request('delete', urlpath, data)

    def request(self, method, urlpath='', data=None):
        headers = {'Content-Type': 'application/json'}
        url = '/'.join([self.url, urlpath])
        logging.debug(
            "Sending METHOD (%(method)s) URL (%(url)s) JSON (%(data)s)",
            {'method': method, 'url': url, 'data': data})
        return self.session.request(
            method, url=url, headers=headers, data=data, timeout=self.timeout)
