from kolibri.deployment.default.settings.base import *  # noqa E402

# Add our authentication middleware.
MIDDLEWARE.append(
    "testapp.middleware.AlwaysAuthenticatedMiddleware"
)
