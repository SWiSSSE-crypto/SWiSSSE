import os
import numpy as np
import matplotlib.pyplot as plt
import math

def extract_stash_size(file_name, N_skip):
    data = []
    file = open(file_name)
    for ii in range(N_skip):
        file.readline()
    for line in file.readlines():
        line = line.split(',')
        data += [int(line[5]) / 1024 / 1024]
    file.close()
    return(data)



stash_size = extract_stash_size('.\\outputs\\search_400000.txt', 8)


plt.hist(stash_size, weights=np.ones(len(stash_size)) / len(stash_size) * 100, bins = 40)

plt.title('Distribution of Stash Size', fontsize=16)
plt.xlabel('Stash Size (MB)', fontsize=14)
plt.ylabel('% of Intermediate States', fontsize=14)

plt.xticks(fontsize=14)
plt.yticks(fontsize=14)

plt.show()
