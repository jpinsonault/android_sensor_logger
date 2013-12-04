import numpy as np
from sklearn import mixture
from sklearn import datasets
from pprint import pprint
import matplotlib.pyplot as plt
import pylab as pl
from sklearn import decomposition
from sklearn import datasets


def main():
    pca()


def gmm_clustering():
    conversion = {
        0: 2,
        1: 0,
        2: 1,
    }

    g = mixture.GMM(n_components=3)

    iris_data = datasets.load_iris()
    diabetes_data = datasets.load_diabetes()
    data = iris_data

    # Generate random observations with two modes centered on 0
    # and 10 to use for training.
    np.random.seed(0)
    obs = np.concatenate((np.random.randn(100, 1), 10 + np.random.randn(300, 1)))
    g.fit(data.data)

    print("Target classification")
    print(data.target)
    results = g.predict(data.data)
    results = [conversion[item] for item in results]

    print("\nResults")
    print(np.array(results))
    compare = [results[i] == data.target[i] for i in range(len(results))]

    accuracy_count = [item for item in compare if item == True]

    print("\nAccuracy: {:.0%}".format(float(len(accuracy_count)) / len(compare)))
    print(max(data.target))

    # print(g.predict([[5.1],  [3.5],  [1.4],  [0.2]]))

    # print(np.round(g.score([[0], [2], [9], [10]]), 2))


def pca():

    iris = datasets.load_iris()
    X = iris.data
    y = iris.target

    plt.figure(1, figsize=(10, 6))
    plt.clf()
    # ax = Axes3D(fig, rect=[0, 0, .95, 1], elev=48, azim=134)

    plt.cla()
    pca = decomposition.PCA(n_components=2)
    print(pca)
    print(X.shape)

    pca.fit(X)
    X = pca.transform(X)
    print(X.shape)

    plt.scatter(X[:, 0], X[:, 1], c=y, )

    plt.show()

if __name__ == '__main__':
    main()