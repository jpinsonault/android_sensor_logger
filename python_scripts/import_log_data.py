import argparse
import sqlite3
from pprint import pprint
from LogEntry import LogEntry
from LogEntry import db
import csv

LOG_FIELDNAMES = ["timestamp", "light_reading", "proximity_reading", 
"x_reading", "y_reading", "z_reading", "activity_name", "activity_confidence"]

args = None


def parse_args():
    global args

    parser = argparse.ArgumentParser()
    parser.add_argument('log_file')
    parser.add_argument('-r', '--reset', action="store_true",
        help="Resets the database before importing")
    parser.add_argument('-e', '--show_errors', action="store_true",
        help="Print entry if there's a problem importing")

    args = parser.parse_args()


def main():
    setup_tables()
    log_data = get_log_data(args.log_file)

    save_data(log_data)


def setup_tables():
    try:
        LogEntry.create_table()
    except (sqlite3.OperationalError):
        pass

    if args.reset:
        LogEntry.drop_table()
        LogEntry.create_table()
    

def save_data(log_data):
    num_entries = len(log_data)
    ten_percent = num_entries / 10

    records_added = 0
    records_skipped = 0

    log_entry_object = LogEntry()

    # Run as a single transaction for speed
    with db.transaction():
        for index, entry in enumerate(log_data):
            if save_entry(entry, log_entry_object):
                records_added += 1
            else:
                records_skipped += 1

            # Print progress to screen
            if index % ten_percent == 0:
                print("{:0.0%} done".format(index / float(num_entries)))

    print_save_results(num_entries, records_added, records_skipped)


def print_save_results(num_entries, records_added, records_skipped):
    print("Tried to import {} records".format(num_entries))
    added_percent = float(records_added) / num_entries
    skipped_percentage = float(records_skipped) / num_entries
    print("{} ({:0.2%}) Successful, {} ({:0.2%}) skipped".format(records_added, added_percent, records_skipped, skipped_percentage))


def save_entry(entry, log_entry_object):
    new_entry = LogEntry(**entry)

    try:
        new_entry.save()
    except sqlite3.IntegrityError:
        if args.show_errors:
            print("Entry skipped: {}".format(entry["timestamp"]))

        return False
    return True


def get_log_data(filename):
    log_dict = []
    with open(filename, 'r') as log_file:
        for line in csv.DictReader(log_file, fieldnames=LOG_FIELDNAMES):
            log_dict.append(line)

    return log_dict

if __name__ == '__main__':
    parse_args()
    main()
