import logging
import os

from android.util import Log
from logging.config import dictConfig
from pathlib import Path

logger = logging.getLogger(__name__)
kolibri_initialized = False

DISABLED_PLUGINS = [
    "kolibri.plugins.learn",
]

REQUIRED_PLUGINS = [
    "kolibri.plugins.app",
    "kolibri_explore_plugin",
]

class AndroidLogHandler(logging.Handler):
    """Logging handler dispatching to android.util.Log

    Converts Python logging records to Android log messages viewable
    with "adb logcat". The handler converts logging levels to log
    priorities, which allows filtering by priority with logcat or other
    Android log analysis tools.
    """

    def __init__(self, tag='TestApp'):
        super().__init__()

        self.tag = tag

    def emit(self, record):
        try:
            msg = self.format(record)
            priority = self.level_to_priority(record.levelno)
            Log.println(priority, self.tag, msg)
        except:  # noqa: E722
            self.handleError(record)

    @staticmethod
    def level_to_priority(level):
        if level >= logging.CRITICAL:
            return Log.ASSERT
        elif level >= logging.ERROR:
            return Log.ERROR
        elif level >= logging.WARNING:
            return Log.WARN
        elif level >= logging.INFO:
            return Log.INFO
        elif level >= logging.DEBUG:
            return Log.DEBUG
        else:
            return Log.VERBOSE


def get_logging_config(LOG_ROOT, debug=False, debug_database=False):
    DEFAULT_LEVEL = "INFO" if not debug else "DEBUG"
    DATABASE_LEVEL = "INFO" if not debug_database else "DEBUG"
    DEFAULT_HANDLERS = ["android", "file"]

    return {
        "version": 1,
        "formatters": {
            "simple": {
                "format": "%(name)s: %(message)s",
            },
            "full": {
                "format": "%(asctime)s %(levelname)-8s %(name)s: %(message)s",
                "datefmt": "%Y-%m-%d %H:%M:%S",
            },
        },
        "handlers": {
            "android": {
                "class": "testapp.main.AndroidLogHandler",
                # Since Android logging already has timestamps and
                # priority levels, they aren't needed here.
                "formatter": "simple",
            },
            "file": {
                # Kolibri uses a customized version of
                # logging.handlers.TimedRotatingFileHandler. We don't
                # want to use that here to avoid importing kolibri too
                # early. IMO, the regular rotating handler based on size
                # is better in the Android case so the total disk space
                # used for logs is managed.
                "class": "logging.handlers.RotatingFileHandler",
                "filename": os.path.join(LOG_ROOT, "kolibri.txt"),
                "maxBytes": 5 << 20,  # 5 Mib
                "backupCount": 5,
                "formatter": "full",
            },
        },
        "loggers": {
            "testapp": {
                "level": "DEBUG",
            },
            # For now, we do not fetch debugging output from this
            # We should introduce custom debug log levels or log
            # targets, i.e. --debug-level=high
            "kolibri.core.tasks.worker": {
                "level": "INFO",
            },
            "django.db.backends": {
                "level": DATABASE_LEVEL,
            },
            "django.template": {
                # Django template debug is very noisy, only log INFO and above.
                "level": "INFO",
            },
        },
        "root": {
            "level": DEFAULT_LEVEL,
            "handlers": DEFAULT_HANDLERS,
        },
    }


def get_empty_logging_config(*args, **kwargs):
    return {"version": 1}


def setup(kolibri_home):
    global kolibri_initialized
    if kolibri_initialized:
        logger.info("Skipping Kolibri setup")
        return

    log_root = os.path.join(kolibri_home, 'logs')
    os.makedirs(log_root, exist_ok=True)
    logging_config = get_logging_config(log_root, debug=True)
    dictConfig(logging_config)

    logger.info("Running Kolibri setup")

    pkg_path = Path(__file__).parent.absolute()

    os.environ['KOLIBRI_HOME'] = kolibri_home
    os.environ['KOLIBRI_DEPLOYMENT_LISTEN_ADDRESS'] = '127.0.0.1'
    os.environ['KOLIBRI_RUN_MODE'] = 'test'
    os.environ['ANDROID_ARGUMENT'] = ''

    autoprovision_path = pkg_path / 'automatic_provision.json'
    if autoprovision_path.is_file():
        os.environ['KOLIBRI_AUTOMATIC_PROVISION_FILE'] = str(autoprovision_path)

    os.environ["KOLIBRI_APPS_BUNDLE_PATH"] = str(pkg_path / "apps")
    os.environ["KOLIBRI_CONTENT_COLLECTIONS_PATH"] = str(pkg_path / "collections")

    # Hardcoded for now
    os.environ["KOLIBRI_INITIAL_CONTENT_PACK"] = "inventor"

    import kolibri.utils.logger
    kolibri.utils.logger.get_default_logging_config = get_empty_logging_config

    from kolibri.utils import env
    env.set_env()

    from kolibri.main import enable_plugin
    from kolibri.main import disable_plugin
    from kolibri.plugins import config as plugins_config

    for plugin_name in DISABLED_PLUGINS:
        if plugin_name in plugins_config.ACTIVE_PLUGINS:
            logger.info(f"Disabling plugin {plugin_name}")
            disable_plugin(plugin_name)
    for plugin_name in REQUIRED_PLUGINS:
        if plugin_name not in plugins_config.ACTIVE_PLUGINS:
            logger.info(f"Enabling plugin {plugin_name}")
            enable_plugin(plugin_name)

    from kolibri.utils.main import initialize
    initialize(debug=True, settings='testapp.settings')

    from kolibri.core.analytics.tasks import schedule_ping
    from kolibri.core.deviceadmin.tasks import schedule_vacuum

    schedule_ping()
    schedule_vacuum()

    kolibri_initialized = True
