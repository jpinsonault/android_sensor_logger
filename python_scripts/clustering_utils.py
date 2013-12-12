import numpy as np
import pylab as pl
from datetime import datetime
from matplotlib.dates import DayLocator, HourLocator, DateFormatter, drange


# Colors used for plotting
color_values = [
    (0,0,0),
    (255,0,0),
    (0,255,0),
    (0,0,255),
    (128,128,128),
    (0,128,128),
    (255,165,0),
    (255,255,255),
    (255,0,255),
]

timestamp_format = '%H:%M:%S %m/%d/%Y'


def smooth_results(data, window_size=30):
    new_data = []

    for index in range(len(data)):
        new_data.append(get_most_common(data, index, window_size))

    return new_data


def get_most_common(data, index, window_size=30):
    start = max(index - window_size, 0)
    end = min(index + window_size, len(data))

    buckets = {}

    for value in data[start:end]:
        buckets[value] = buckets.get(value, 0) + 1

    return max(buckets.iterkeys(), key=(lambda key: buckets[key]))


def plot_clusters(clusterer):
    trim_value = 20

    clusterer.fit()
    predictions = clusterer.predict()

    sensor_data = clusterer.data_array[::trim_value]
    prediction_trimmed = predictions[::trim_value]

    colors = [[value/255.0 for value in color_values[prediction_trimmed[index]]] for index, row in enumerate(sensor_data)]

    fig = pl.figure()
    # pca = PCA(n_components=2)
    # flattened = pca.fit(sensor_data).transform(sensor_data)
    ax = fig.gca(projection='3d')
    ax.scatter(sensor_data[:,0], sensor_data[:,1], sensor_data[:,2], c=colors)
    pl.show()


def filter_positions(clusterer, positions):
    clusterer.fit()
    predictions = clusterer.predict()
    predictions = smooth_results(predictions, window_size=150)
    sensor_data = clusterer.accelerometer_data
    converter = clusterer.classify()

    # start_index, end_index = get_start_end(time_start, time_end, sensor_data)

    filtered_list = [entry for index, entry in enumerate(sensor_data) if match_positions(predictions[index], converter, positions)]
    return filtered_list


def time_spent(clusterer, positions):
    clusterer.fit()
    predictions = clusterer.predict()
    predictions = smooth_results(predictions, window_size=150)
    sensor_data = clusterer.accelerometer_data
    converter = clusterer.classify()

    # start_index, end_index = get_start_end(time_start, time_end, sensor_data)
    
    time_spent = 0

    event_start = None
    for index, prediction in enumerate(predictions):
        if match_positions(prediction, converter, positions):
            if event_start is None:
                event_start = sensor_data[index]["timestamp"]
        else:
            if event_start is not None:
                event_end = sensor_data[index - 1]["timestamp"]
                time_difference = datetime.strptime(event_end, timestamp_format) - datetime.strptime(event_start, timestamp_format)
                time_spent += time_difference.total_seconds()

                event_start = None

    return time_spent


def bin_by_hour(clusterer, positions):
    filtered = filter_positions(clusterer, positions)

    buckets = {hour: 0 for hour in range(24)}

    for entry in filtered:
        hour = datetime.strptime(entry["timestamp"], timestamp_format).hour
        buckets[hour] = buckets.get(hour, 0) + 1

    return buckets


def get_start_end(time_start, time_end, sensor_data):
    if time_start is None:
        start_index = 0
    else:
        start_index = get_first_time(time_start, sensor_data)
    if time_end is None:
        end_index = len(sensor_data) - 1
    else:
        end_index = get_first_time(time_end, sensor_data)

    return start_index, end_index


def match_positions(prediction, converter, positions):
    return any([prediction in converter[position] for position in positions])


def get_first_time(time, sensor_data):
    search_time = datetime.strptime(time, timestamp_format)

    for index, data in enumerate(sensor_data):
        if datetime.strptime(data["timestamp"], timestamp_format) > search_time:
            return max(0, index - 1)
    else:
        raise Exception("No time greater than {} found".format(time))


def plot_data(data, clusterer):
    predictions = clusterer.predict(to_numpy(data))
    timestamps = [datetime.strptime(row["timestamp"], timestamp_format) for row in data]

    fig, ax = pl.subplots()
    ax.plot_date(timestamps, predictions, 'b')

    ax.xaxis.set_minor_locator(HourLocator(np.arange(0,25,6)))
    ax.xaxis.set_minor_formatter(DateFormatter('%H'))

    ax.xaxis.set_major_locator(DayLocator())
    ax.xaxis.set_major_formatter(DateFormatter('%a'))
    ax.fmt_xdata = DateFormatter('%H:%M:%S')
    fig.autofmt_xdate()
    pl.show()


def to_numpy(data):
    """
        Returns a numpy array of the log data to cluster on
    """
    fields = [
        "x", "y", "z",
        "proximity"
    ]
    return np.array([[row[field] for field in fields] for row in data])