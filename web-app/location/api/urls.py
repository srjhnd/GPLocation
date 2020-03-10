from django.conf.urls import url
from location.api.views import api_latest_location

app_name = 'location'

urlpatterns = [
    url('latestLocation/', api_latest_location, name="latestLocation"),
]
