import numpy as np
from sklearn import mixture
import matplotlib.pyplot as plt
from LogEntry import LogEntry
from datetime import datetime
from matplotlib.dates import DayLocator, HourLocator, DateFormatter, drange
from numpy import arange
from os.path import isfile
import json


def cache_dict(filename):
    """
        For functions that retrieve data in a json-writable format
        Checks if filename exists, if so gets the data from the file. 
        If not, gets the data normally then caches it in the file
    """
    def wrapper(function):
        def wrapped(*args):
            data = None
            if isfile(filename):
                with open(filename, 'r') as in_file:
                    data = json.load(in_file)
            else:
                data = function(*args)
                with open(filename, 'w') as out_file:
                    out_file.write(json.dumps(data))
            return data
        return wrapped
    return wrapper


NEAR = 0.0
FAR = 5.0

# (x,y,z,proximity)
REFERENCE_DATA = {
    "flat-screen-down": [(-0.15322891, -0.15322891, -9.80665, NEAR)],
    "flat-screen-up": [(0.15322891, -0.15322891, 9.959879, FAR)],
    "in-hand-portrait": [
        (-0.30645782, 5.8226986, 7.8146744, FAR),
        (0.61291564, 3.064578, 9.346964, FAR),
        (0.7661445, 3.064578, 9.346964, FAR),
        (0.61291564, 3.217807, 9.346964, FAR),
    ],
    "in-hand-landscape": [
        (-5.363012, 0.61291564, 8.580819, FAR),
        (-5.209783, 0.7661445, 8.42759, FAR),
        (-5.209783, 0.45968673, 8.734048, FAR),
    ],
    "standing-pocket-upside-down": [
        (2.2984335, -8.274361, -0.91937345, NEAR),
        (-2.7581203, -9.653421, -0.15322891, NEAR),
        (0.91937345, -9.80665, 0.15322891, NEAR)
    ],
    "standing-pocket-rightside-up": [
        (-3.371036, 8.887277, 0.15322891, NEAR),
        (3.6774938, 8.734048, 0.0, NEAR)
    ],
    "sitting-pocket-upside-down": [
        (7.3549876, -1.3790601, -6.2823853, NEAR),
        (-7.3549876, -2.2984335, 6.2823853, NEAR)
    ],
    "sitting-pocket-rightside-up": [
        (-7.3549876, 0.91937345, -6.2823853, NEAR),
        (7.8146744, 1.0726024, 6.129156, NEAR)
    ],
}

class OrientationClusterer:
    """Class for clustering accelerometer data and retrieving data"""

    time_stamp_format = '%H:%M:%S %m/%d/%Y'

    def __init__(self, gmm, cluster_on=[""]):
        self.gmm = gmm
        self.accelerometer_data = self.get_accelerometer_data()
        self.predictions = None
        # Format it in a numpy array
        self.data_array = self.to_numpy()

        self.is_fitted = False
        self.is_predicted = False

    @cache_dict("accelerometer_data.json")
    def get_accelerometer_data(self):
        # Sort the data
        all_data = sorted(LogEntry.select(), key=lambda row: datetime.strptime(row.timestamp, self.time_stamp_format))
        accelerometer_data = [{"timestamp": row.timestamp, "light": row.light_reading, "proximity": row.proximity_reading,
            "x": row.x_reading, "y": row.y_reading, "z": row.z_reading} for row in all_data]

        return accelerometer_data

    def fit(self):
        # Skip if already fitted
        if self.is_fitted:
            return
        self.gmm.fit(self.data_array)
        self.is_fitted = True


    def to_numpy(self):
        """
            Returns a numpy array of the log data to cluster on
        """
        fields = [
            "x", "y", "z",
            "proximity"
        ]
        return np.array([[row[field] for field in fields] for row in self.accelerometer_data])

    def predict(self, data=None):
        if data is None:
            data = self.data_array
        self.predictions = self.gmm.predict(data)

        return self.predictions

    def classify(self):
        """
            Assign names to the clusters
            Returns a dict to translate the cluster number to a name
        """
        self.fit()

        ids_to_names = {name: set() for name in REFERENCE_DATA}

        for name, reference_points in REFERENCE_DATA.iteritems():
            for point in reference_points:
                prediction = self.predict([point])
                ids_to_names[name] = ids_to_names[name].union(prediction)

        return ids_to_names
