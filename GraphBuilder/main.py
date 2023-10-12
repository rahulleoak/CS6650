# pip install pandas matplotlib numpy


import pandas as pd
import matplotlib.pyplot as plt

# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # Read the CSV data
    df = pd.read_csv('records.csv')
    df2 = pd.read_csv('recordsGo.csv')
    # Convert Start Time to seconds from the beginning
    df['Start Time'] = (df['Start Time'] - df['Start Time'].min()) // 1000
    df2['Start Time'] = (df2['Start Time'] - df2['Start Time'].min()) // 1000
    # Group by the second and count the number of requests
    throughput = df.groupby('Start Time').size()
    throughput2 = df2.groupby('Start Time').size()
    # Plotting
    plt.plot(throughput.index, throughput.values, label='Java')
    plt.plot(throughput2.index, throughput2.values, label='Go')
    plt.scatter(throughput.index, throughput.values, color='blue', marker='o', s=10, label='Java')
    plt.scatter(throughput2.index, throughput2.values, color='red', marker='s', s=10, label='Go')
    plt.legend(["Java", "Golang"])
    plt.xlabel('Time (seconds)')
    plt.ylabel('Requests/Second')
    plt.title('Throughput over Time Comparison of Java vs Golang')
    plt.grid(True)
    plt.show()

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
