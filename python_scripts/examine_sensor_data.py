import argparse
from  OrientationClusterer import OrientationClusterer
from pprint import pprint
from sklearn import mixture
from clustering_utils import smooth_results
from clustering_utils import plot_clusters
from clustering_utils import filter_positions
from clustering_utils import plot_data
from clustering_utils import time_spent
from clustering_utils import bin_by_hour
from sklearn.decomposition import PCA
import pylab as pl
from mpl_toolkits.mplot3d import axes3d
from datetime import datetime


args = None


def parse_args():
    global args

    parser = argparse.ArgumentParser()

    args = parser.parse_args()


def main():
    clusterer = OrientationClusterer(mixture.GMM(n_components=9, n_iter=500))
    # plot_clusters(clusterer)
    clusterer.fit()
    predictions = clusterer.predict()

    for row in predictions:
        print(row)
    print("###")
    for row in smooth_results(predictions, 150):
        print(row)


if __name__ == '__main__':
    main()
