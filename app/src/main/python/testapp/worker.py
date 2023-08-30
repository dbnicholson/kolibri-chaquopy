import logging
from kolibri.utils.server import BaseKolibriProcessBus
from kolibri.utils.server import ServicesPlugin

logger = logging.getLogger(__name__)


class WorkerProcessBus(BaseKolibriProcessBus):
    def __init__(self, activity, *args, **kwargs):
        super().__init__(*args, **kwargs)

        services_plugin = ServicesPlugin(self)
        services_plugin.subscribe()

    def start(self):
        logger.info('Starting bus')
        self.graceful()

    def stop(self):
        logger.info('Stopping bus')
        self.transition('EXITED')
