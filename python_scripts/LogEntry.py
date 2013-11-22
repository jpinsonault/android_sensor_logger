from peewee import *

db = SqliteDatabase('sensordata.db')

class LogEntry(Model):
    timestamp = DateField(null=True, unique=True)
    light_reading = FloatField(null=True)
    proximity_reading = FloatField(null=True)
    x_reading = FloatField(null=True)
    y_reading = FloatField(null=True)
    z_reading = FloatField(null=True)
    activity_name = IntegerField(null=True)
    activity_confidence = IntegerField(null=True)

    class Meta:
        database = db
