import os
import matplotlib.pyplot as plt
import math

def extract_plaintext_query_times(file_name, N_skip, offSet=0):
    data = []
    file = open(file_name)
    for ii in range(N_skip):
        file.readline()
    for line in file.readlines():
        line = line.split(',')
        data += [(int(line[1+offSet]), int(line[2+offSet])/10**6)]

    file.close()

    return(data)

def extract_swissse_query_times(file_name, N_skip, offSet=0):
    data = []
    file = open(file_name)
    for ii in range(N_skip):
        file.readline()
    for line in file.readlines():
        line = line.split(',')
        data += [(int(line[1+offSet]), int(line[2+offSet])/10**6 + int(line[3+offSet])/10**6)]

    file.close()

    return(data)


def smoothing(times, grouping):
    data = {}
    for time in times:
        x = math.ceil(time[0] / grouping) * grouping
        if x not in data:
            data[x] = []
        data[x] += [time[1]]

    results = {}
    for x in data:
        if len(data[x]) > 5:
            data[x] = sorted(data[x])
        results[x] = sum(data[x]) / len(data[x])

    xs = sorted(results.keys())
    ys = [results[x] for x in xs]
    return (xs, ys)


grouping = 200
time_plaintext  = extract_plaintext_query_times('./outputs/plaintext_400000.txt', 7, offSet=0)
x_plaintext, y_plaintext = smoothing(time_plaintext, grouping)


time_swissse    = extract_swissse_query_times('./outputs/search_400000.txt', 8, offSet=0)
x_swissse, y_swissse = smoothing(time_swissse, grouping)



# plot search time
plt.plot(x_plaintext, y_plaintext)
plt.plot(x_swissse, y_swissse)
plt.xlim([-500,20500])

plt.yscale('log')

plt.title("Insertion Time", fontsize=16)
plt.xlabel('Min frequency of the keywords in the document', fontsize=14)
plt.ylabel('Insertion Time (ms)',fontsize=14)
plt.legend(('Plaintext', 'SWiSSSE'), fontsize=14)

plt.xticks(fontsize=12)
plt.yticks(fontsize=14)

plt.show()
