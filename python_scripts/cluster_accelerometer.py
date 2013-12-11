import argparse
from  OrientationClusterer import OrientationClusterer
from pprint import pprint
from sklearn import mixture
from clustering_utils import smooth_results
from sklearn.decomposition import PCA
import pylab as pl
from mpl_toolkits.mplot3d import axes3d

args = None

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

def parse_args():
    global args

    parser = argparse.ArgumentParser()

    args = parser.parse_args()


def main():
    clusterer = OrientationClusterer(mixture.GMM(n_components=9, n_iter=500))
    
    # plot_clusters(clusterer)
    clusterer.fit()
    predictions = clusterer.predict()
    converter = clusterer.classify()

    pprint(converter)

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


if __name__ == '__main__':
    main()
