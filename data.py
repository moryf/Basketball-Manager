import pandas as pd

df1 = pd.read_csv('players 2025.csv')
df2 = pd.read_csv('playes 2025 -1.csv')
df3 = pd.read_csv('players 2025 -2.csv')


df1.head()
df2.head()
df3.head()

#%%
df1=df1.merge(
    df2,
    how='left',
    left_on='Player-additional',
    right_on='Player-additional',
    suffixes=('_adv', '_basic'))
df1=df1.merge(
    df3,
    how='left',
    left_on='Player-additional',
    right_on='-9999',
    suffixes=('', '_shot'))

#%%
# keep only relevant columns like this :name "Jamal Murray"
#                                  :usage-rate 27.5, :shot-dist-2p 0.655, :shot-dist-3p 0.345,
#                                  :fg-perc-2p 0.523, :fg-perc-3p 0.425, :orb-perc 2.1, :drb-perc 12.5,
#                                  :ast-perc-2p 0.45, :ast-perc-3p 0.65, :ast-perc 30.1, :tov-pct 9.9, :ftr 0.226, :ft-perc 0.88,
#                                  :obpm 4.3, :dbpm -0.7

df1 = df1[['Player-additional','Player', 'USG%', '2P_shot', '3P_shot', '2P%', '3P%', 'ORB%', 'DRB%', '2P.2', '3P.2', 'AST%', 'TOV%', 'FTr', 'FT%', 'OBPM', 'DBPM', 'PER▼']]



#%%
df1['PER▼'].mean()


#%%
df1.to_csv('players_final.csv', index=False)