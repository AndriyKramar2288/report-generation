import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

# Завдання 1: Дослідження одновимірної випадкової величини X1
np.random.seed(42)
sigma = 5
n = 30
X1 = np.random.normal(loc=0, scale=sigma, size=n)

X1_sorted = np.sort(X1)

# --- Діаграма накопичених частот ---
plt.figure(figsize=(8, 4))
plt.step(X1_sorted, np.arange(1, n + 1) / n, where='mid', label='Накопичена частота')
plt.xlabel('X1')
plt.ylabel('Частота')
plt.title('Діаграма накопичених частот')
plt.legend()
plt.grid()
plt.savefig('diagram_accumulated.png') # Зберігаємо ПЕРЕД показом
plt.show()

# --- Гістограма вибірки ---
plt.figure(figsize=(8, 4))
plt.hist(X1, bins='auto', alpha=0.7, color='blue', edgecolor='black')
plt.xlabel('X1')
plt.ylabel('Частота')
plt.title('Гістограма вибірки X1')
plt.grid()
plt.savefig('histogram_x1.png') # Зберігаємо ПЕРЕД показом
plt.show()

mean_X1 = np.mean(X1)
var_X1 = np.var(X1, ddof=1)
sigma_X1 = np.std(X1, ddof=1)

print(f'Математичне сподівання: {mean_X1:.4f}')
print(f'Дисперсія: {var_X1:.4f}')
print(f'Середньоквадратичне відхилення: {sigma_X1:.4f}')

# Завдання 2: Дослідження двовимірної сукупності
Y1 = np.random.normal(loc=0, scale=sigma, size=n)

# --- Поле розсіяння ---
plt.figure(figsize=(6, 6))
plt.scatter(X1, Y1, alpha=0.7, color='red', edgecolor='black')
plt.xlabel('X1')
plt.ylabel('Y1')
plt.title('Поле розсіяння для X1 та Y1')
plt.grid()
plt.savefig('scatter_plot.png') # Зберігаємо ПЕРЕД показом
plt.show()

bin_x = np.histogram_bin_edges(X1, bins='auto')
bin_y = np.histogram_bin_edges(Y1, bins='auto')

H, x_edges, y_edges = np.histogram2d(X1, Y1, bins=[bin_x, bin_y])
df_distribution = pd.DataFrame(H, index=x_edges[:-1], columns=y_edges[:-1])
print('Таблиця двовимірного розподілу:')
print(df_distribution)

correlation_coefficient = np.corrcoef(X1, Y1)[0, 1]
print(f'Коефіцієнт кореляції: {correlation_coefficient:.4f}')