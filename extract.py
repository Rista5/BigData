import pandas as pd

file = pd.read_csv("~/big-data/data.csv")

columns = ['Severity','Distance(mi)','Street','City','State','Country','Temperature(F)','Wind_Chill(F)','Humidity(%)','Pressure(in)','Visibility(mi)','Wind_Speed(mph)','Precipitation(in)','Weather_Condition']

for c in columns:
    file = file[file[c].notnull()]

print('file shape')
print(file.shape)
file.to_csv('~/big-data/filt-data.csv', index=False)