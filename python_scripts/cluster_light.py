import numpy as np
import argparse
from pprint import pprint
from sklearn import mixture
from sklearn import datasets
import matplotlib.pyplot as plt
from sklearn import decomposition
from LogEntry import LogEntry
from LogEntry import db
from datetime import datetime
from matplotlib.dates import DayLocator, HourLocator, DateFormatter, drange
from numpy import arange

args = None
format_string = '%H:%M:%S %m/%d/%Y'

def parse_args():
    global args

    parser = argparse.ArgumentParser()

    args = parser.parse_args()


def main():
    g = mixture.GMM(n_components=4)

    log_entries = load_light()
    light_data = [min(row.light_reading, 120) for row in log_entries]
    timestamps = [datetime.strptime(row.timestamp, format_string) for row in log_entries]

    g.fit(light_data)

    predictions = predict(g, light_data)

    light_dict = {}
    inside = bin_by_hour(timestamps, predictions, [0,1])
    outside = bin_by_hour(timestamps, predictions, [2,3])

    pprint(inside)
    pprint(outside)



def plot_light_data(timestamps, predictions):
    fig, ax = plt.subplots()
    ax.plot_date(timestamps, predictions, 'b')

    ax.xaxis.set_minor_locator(HourLocator(arange(0,25,6)))
    ax.xaxis.set_minor_formatter(DateFormatter('%H'))

    ax.xaxis.set_major_locator(DayLocator())
    ax.xaxis.set_major_formatter(DateFormatter('%a'))
    ax.fmt_xdata = DateFormatter('%H:%M:%S')
    fig.autofmt_xdate()
    plt.show()


def bin_by_hour(timestamps, predictions, clusters):
    filtered = [timestamps[index] for index, entry in enumerate(timestamps) if predictions[index] in clusters]

    buckets = {hour: 0 for hour in range(24)}

    for time in filtered:
        hour = time.hour
        buckets[hour] = buckets.get(hour, 0) + 1

    return buckets


def predict(gmm, data):
    results = gmm.predict(data)
    smoothed = smooth_results(results)
    converter = make_converter(gmm, smoothed)
    
    return [converter[value] for value in smoothed]


def load_light():
    light_data = LogEntry.select()
    return sorted(light_data, key=lambda row: datetime.strptime(row.timestamp, format_string))


def smooth_results(data):
    new_data = []

    for index in range(len(data)):
        new_data.append(get_most_common(data, index))

    return new_data


def make_converter(gmm, data):
    converter = {}
    means = [[index, value[0]] for index, value in enumerate(gmm.means_)]
    
    for index, mean in enumerate(sorted(means, key=lambda means: means[1])):
        converter[mean[0]] = index

    return converter


def get_most_common(data, index):
    window_size = 100

    start = max(index - window_size, 0)
    end = min(index + window_size, len(data))

    buckets = {}

    for value in data[start:end]:
        buckets[value] = buckets.get(value, 0) + 1

    return max(buckets.iterkeys(), key=(lambda key: buckets[key]))



if __name__ == '__main__':
    main()