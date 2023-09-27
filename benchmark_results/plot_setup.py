import os
import matplotlib.pyplot as plt

def extract_setup_time(file_name, N_skip):
    data = {}
    file = open(file_name)

    for ii in range(N_skip):
        file.readline()

    data = int(file.readline().split(':')[1]) / 10**9
    file.close()
    return(data)



data_plaintext = {}
for N_emails in [10000, 50000, 100000, 200000, 400000]:
    filename = f'./outputs/plaintext_{N_emails}.txt'
    data_plaintext[N_emails] = extract_setup_time(filename, 4)

data_swissse = {}
for N_emails in [10000, 50000, 100000, 200000, 400000]:
    filename = f'./outputs/search_{N_emails}.txt'
    data_swissse[N_emails] = extract_setup_time(filename, 4)



# plot KDP
xs1 = sorted(data_plaintext.keys())
index_plaintext   = [data_plaintext[x] for x in xs1]

xs2 = sorted(data_swissse.keys())
index_swissse     = [data_swissse[x] for x in xs2]

plt.scatter(xs1, index_plaintext)
plt.scatter(xs2, index_swissse)
plt.locator_params(axis='x', nbins=6)
plt.yscale('log')

plt.title('Setup Time', fontsize=16)
plt.xlabel('# documents', fontsize=14)
plt.ylabel('Time (s)', fontsize=14)

plt.xticks(fontsize=14)
plt.yticks(fontsize=14)


plt.legend(labels = ('Plaintext', 'SWiSSSE'), fontsize=14, loc='best')
plt.show()
