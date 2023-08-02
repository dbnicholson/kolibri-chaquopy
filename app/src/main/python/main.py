import logging
import os

logger = logging.getLogger(__name__)
kolibri_initialized = False


def setup(kolibri_home):
    global kolibri_initialized
    if kolibri_initialized:
        logger.info("Skipping Kolibri setup")
        return

    logger.info("Running Kolibri setup")

    os.environ['KOLIBRI_HOME'] = kolibri_home
    os.environ['KOLIBRI_DEPLOYMENT_LISTEN_ADDRESS'] = '127.0.0.1'
    os.environ['KOLIBRI_RUN_MODE'] = 'test'
    os.environ['ANDROID_ARGUMENT'] = ''

    from kolibri.utils import env
    env.set_env()

    from kolibri.utils.main import initialize
    initialize(debug=True)

    from kolibri.core.analytics.tasks import schedule_ping
    from kolibri.core.deviceadmin.tasks import schedule_vacuum

    schedule_ping()
    schedule_vacuum()

    kolibri_initialized = True
