import logging
from kolibri.utils.server import BaseKolibriProcessBus
from kolibri.utils.server import KolibriServerPlugin
from kolibri.utils.server import ZipContentServerPlugin

logger = logging.getLogger(__name__)


class AppProcessBus(BaseKolibriProcessBus):
    def __init__(self, activity, *args, **kwargs):
        super().__init__(*args, **kwargs)

        server_plugin = KolibriServerPlugin(self, self.port)
        server_plugin.subscribe()

        zip_server_plugin = ZipContentServerPlugin(self, self.zip_port)
        zip_server_plugin.subscribe()

    def start(self):
        logger.info('Starting bus')
        self.graceful()

    def stop(self):
        logger.info('Stopping bus')
        self.transition('EXITED')

    def get_url(self):
        if self.state != 'RUN':
            raise Exception('Bus not running')
        return f'http://127.0.0.1:{self.port}/'
