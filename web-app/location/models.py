from django.db import models

# Create your models here.


class Location(models.Model):
    location_id = models.CharField(max_length=10, default='0')
    longitude = models.DecimalField(max_digits=9, decimal_places=6)
    latitude = models.DecimalField(max_digits=9, decimal_places=6)
    time = models.CharField(max_length=100)

    def __str__(self):
        return "id:" + self.location_id + " longitude:" + str(self.longitude) + " latitude:" + str(self.latitude) + " time:" + str(self.time)
