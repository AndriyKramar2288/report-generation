
import pandas as pd
import numpy as np
from pathlib import Path
import matplotlib.pyplot as plt

plt.plot()
plt.savefig("aboba.jpg")

# НАЛАШТУВАННЯ (Варіант 5)
VARIANT = 5
INPUT_FILE = f"variant{VARIANT:02d}.csv"
DUP_KEY = ["sale_id"]
OUTLIER_COL = "amount"

def detect_outliers_iqr(df, col):
    Q1 = df[col].quantile(0.25)
    Q3 = df[col].quantile(0.75)
    IQR = Q3 - Q1
    lower = Q1 - 1.5 * IQR
    upper = Q3 + 1.5 * IQR
    return lower, upper

def main():
    print(f"--- Lab 4: Data Cleaning (Variant {VARIANT}) ---")
    df = pd.read_csv(INPUT_FILE)
    
    print("1. Initial Shape:", df.shape)
    print("   Nulls:\n", df.isnull().sum().to_dict())
    
    # 1. Fill Missing
    # region -> mode
    mode_reg = df["region"].mode()[0]
    df["region"] = df["region"].fillna(mode_reg)
    
    # amount -> median
    med_amt = df["amount"].median()
    df["amount"] = df["amount"].fillna(med_amt)
    
    print("\n2. After Filling NaNs:")
    print("   Nulls:", df.isnull().sum().sum())
    
    # 2. Duplicates
    dups = df.duplicated(subset=DUP_KEY).sum()
    print(f"\n3. Found {dups} duplicates by {DUP_KEY}. Dropping...")
    df = df.drop_duplicates(subset=DUP_KEY, keep="first")
    
    # 3. Outliers (IQR) -> Mark + Drop
    lo, hi = detect_outliers_iqr(df, OUTLIER_COL)
    print(f"\n4. Outlier detection (IQR) for '{OUTLIER_COL}': [{lo}, {hi}]")
    
    # Identify outliers
    outliers_mask = (df[OUTLIER_COL] < lo) | (df[OUTLIER_COL] > hi)
    outliers_count = outliers_mask.sum()
    
    # Drop them
    df_clean = df[~outliers_mask].copy()
    
    print(f"   Found {outliers_count} outliers. Dropped.")
    print(f"   Final Shape: {df_clean.shape}")
    
    # Save
    df_clean.to_csv(f"clean_variant{VARIANT:02d}.csv", index=False)
    
    # Report
    rep = [
        f"Lab 4 Report (Variant {VARIANT})",
        f"Initial rows: 10 (hardcoded sample)", # based on file content known
        f"Duplicates dropped: {dups}",
        f"Imputation: region='{mode_reg}', amount={med_amt}",
        f"IQR Bounds: {lo} .. {hi}",
        f"Outliers dropped: {outliers_count}",
        f"Final rows: {len(df_clean)}"
    ]
    Path(f"report_variant{VARIANT:02d}.txt").write_text("\n".join(rep), encoding="utf-8")
    print("\nReport saved.")

if __name__ == "__main__":
    main()
