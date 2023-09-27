import os
import numpy as np
import matplotlib.pyplot as plt
import math

def extract_writeback_times(file_name, N_skip):
    data = []
    file = open(file_name)
    for ii in range(N_skip):
        file.readline()
    for line in file.readlines():
        line = line.split(',')
        data += [int(line[4])/10**9]
    file.close()
    return(data)



writeback_swissse = extract_writeback_times('./outputs/search_400000.txt', 8)
plt.hist(writeback_swissse, weights=np.ones(len(writeback_swissse)) / len(writeback_swissse) * 100, bins = 20)

plt.title('Distribution of Write-back Time', fontsize=16)
plt.xlabel('Write-back time (s)', fontsize=14)
plt.ylabel('% of write-backs', fontsize=14)

plt.xticks(fontsize=14)
plt.yticks(fontsize=14)

plt.show()
