import argparse
import sqlite3
from pprint import pprint
from LogEntry import LogEntry
from LogEntry import db
import csv
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import dateutil
from datetime import datetime

LOG_FIELDNAMES = ["timestamp", "light_reading", "proximity_reading", 
"x_reading", "y_reading", "z_reading", "activity_name", "activity_confidence"]

args = None


def parse_args():
    global args

    parser = argparse.ArgumentParser()
    # parser.add_argument('-e', '--show_errors', action="store_true",
    #     help="Print entry if there's a problem importing")

    args = parser.parse_args()


def main():
    plot_data()

    # log_data = LogEntry.select().order_by(LogEntry.timestamp)

    # pprint([row.timestamp for row in log_data[:100]])


def get_light_data():
    light_data = LogEntry.select()

    return light_data


def plot_data():
    format_string = '%H:%M:%S %m/%d/%Y'

    log_data = LogEntry.select().order_by(LogEntry.timestamp)
    # log_data = [row for row in log_data]
    log_data = sorted(log_data, key=lambda row: datetime.strptime(row.timestamp, format_string))

    light_readings = [row.light_reading for row in log_data]
    light_readings = [min(reading, 100.0) for reading in light_readings]
    timestamps = [datetime.strptime(row.timestamp, format_string) for row in log_data]

    fig = plt.figure()
    # plt.figure(1, figsize=(10, 6))
    plt.clf()
    plt.cla()

    plt.plot_date(timestamps, light_readings, 'b')
    fig.autofmt_xdate()
    plt.show()


if __name__ == '__main__':
    parse_args()
    main()
