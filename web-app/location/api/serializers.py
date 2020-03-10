from django.contrib.auth.models import User, Group
from rest_framework import serializers
from location.models import Location

class LocationSerializer(serializers.ModelSerializer):
    class Meta:
        model = Location
        fields = ('longitude', 'latitude', 'time')