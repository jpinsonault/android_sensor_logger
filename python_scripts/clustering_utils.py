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
