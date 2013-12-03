from peewee import *

db = SqliteDatabase('sensordata.db')


class LogEntry(Model):
    timestamp = DateField(unique=True)
    light_reading = FloatField()
    proximity_reading = FloatField()
    x_reading = FloatField()
    y_reading = FloatField()
    z_reading = FloatField()
    activity_name = IntegerField(null=True)
    activity_confidence = IntegerField(null=True)

    class Meta:
        database = db
