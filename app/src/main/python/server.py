import logging
from kolibri.utils.server import BaseKolibriProcessBus
from kolibri.utils.server import PROCESS_CONTROL_FLAG
from kolibri.utils.server import KolibriServerPlugin
from kolibri.utils.server import STATUS_STOPPED
from kolibri.utils.server import STOP
from kolibri.utils.server import wait_for_status
from kolibri.utils.server import ZipContentServerPlugin
from magicbus.plugins import SimplePlugin

logger = logging.getLogger(__name__)


class AppPlugin(SimplePlugin):
    def __init__(self, bus, activity):
        self.bus = bus
        self.activity = activity
        self.bus.subscribe("SERVING", self.SERVING)

    def SERVING(self, port):
        start_url = f"http://127.0.0.1:{port}/"
        logger.info(f"Ready on {start_url}")
        self.activity.loadUrl(start_url)
        self.activity.setServerReady()


def start(activity):
    kolibri_bus = BaseKolibriProcessBus()

    kolibri_server = KolibriServerPlugin(
        kolibri_bus,
        kolibri_bus.port,
    )
    kolibri_server.subscribe()

    alt_port_server = ZipContentServerPlugin(
        kolibri_bus,
        kolibri_bus.zip_port,
    )
    alt_port_server.subscribe()

    app_plugin = AppPlugin(kolibri_bus, activity)
    app_plugin.subscribe()

    kolibri_bus.run()


def stop():
    with open(PROCESS_CONTROL_FLAG, "w") as f:
        f.write(STOP)
    wait_for_status(STATUS_STOPPED, timeout=10)
