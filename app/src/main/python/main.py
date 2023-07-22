import logging
import os
import time

logger = logging.getLogger(__name__)


def setup(kolibri_home):
    os.environ['KOLIBRI_HOME'] = kolibri_home
    os.environ['KOLIBRI_DEPLOYMENT_LISTEN_ADDRESS'] = '127.0.0.1'
    os.environ['ANDROID_ARGUMENT'] = ''

    from kolibri.utils import env
    env.set_env()

    from kolibri.utils.main import initialize
    initialize(debug=True)


def start():
    from kolibri.utils import server
    server.start()


def stop():
    from kolibri.utils import server
    server.stop()


def get_url():
    from kolibri.utils import server

    deadline = time.time() + 10
    while True:
        try:
            _, _, port = server.get_status()
        except server.NotRunning:
            if time.time() >= deadline:
                raise
            time.sleep(0.1)
        else:
            return f'http://127.0.0.1:{port}/'
