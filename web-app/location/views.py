from django.shortcuts import render
from .models import Location
# Create your views here.
def index(request):
    latest_location = Location.objects.filter(location_id='0')[0]
    context = {'latest_location': latest_location}
    return render(request, 'location/index.html', context)