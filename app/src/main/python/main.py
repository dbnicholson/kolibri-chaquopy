import logging
import os

logger = logging.getLogger(__name__)


def setup(kolibri_home):
    os.environ['KOLIBRI_HOME'] = kolibri_home
    os.environ['KOLIBRI_DEPLOYMENT_LISTEN_ADDRESS'] = '127.0.0.1'

    from kolibri.utils import env
    env.set_env()
    import kolibri
    finder = kolibri.__loader__.finder
    finder.extract_dir("kolibri/dist")
    from kolibri.utils import server


def start():
    from kolibri.utils import server
    server.start()


def stop():
    from kolibri.utils import server
    server.stop()


def get_url():
    from kolibri.utils import server
    _, _, port = server.get_status()
    return f'http://127.0.0.1:{port}/'
