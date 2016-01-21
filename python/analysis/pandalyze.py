import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import sys

episode_map = {"KMTE20000114": "ep1",
               "KMTE20000214": "ep2",
               "KMTE20000314": "ep3",
               "KMTE20000414": "ep4",
               "KMTE20000514": "ep5",
               "KMTE20000614": "ep6"}


def plot_episode_views(df):
    """
    Visualize the proportion of users who watched the entire episode.
    """
    viewers = df.groupby("programId").userId.nunique()
    fig, ax = plt.subplots(1, 1)
    ax.get_xaxis().set_visible(False)
    viewers.plot(table=True, kind='bar')
    plt.title("Viewers per episode")
    plt.savefig("/tmp/viewer_per_episode.png")
    plt.show()


def plot_watched_seconds(views):
    """
    Plot a graph where the x-axis represents seconds watched in an episode, and the
    y-axis represents the proportion of viewers who watched (at least) x many seconds.
    """
    one_episode_sessions = views.drop_duplicates('visitStartTime', keep=False)
    one_ep_sorted = one_episode_sessions.sort_values(by='timeWithinVisit', ascending=False)
    one_ep_sorted['timeWithinVisit'] /= 1000
    one_ep_sorted['timeWithinVisit'] = one_ep_sorted['timeWithinVisit'].apply(lambda x: min(x, 1800))
    sorted_episodes = episode_map.values()
    sorted_episodes.sort()
    for episode in sorted_episodes:
        episode_df = one_ep_sorted.loc[one_ep_sorted['programId'].isin([episode])]
        y = np.linspace(0, 1, num=100)
        x = []
        for prop in y:
            x.append(episode_df['timeWithinVisit'].quantile(1 - prop))
        plt.title("Unge Lovende")
        plt.xlabel('Seconds watched')
        plt.ylabel('Proportion of population')
        plt.axis([0, 1800, 0.6, 1.0])
        plt.plot(x, y)
    plt.legend(sorted_episodes)
    plt.show()


def plot_watched_episodes_in_session(df):
    """
    Plot the proportion of the sessions that consists of one episode view, two episode views, etc.
    """
    session_lengths = df['visitStartTime'].groupby(df['userId']).value_counts()
    session_length_freq = session_lengths.value_counts()
    sum_all_sessions = float(session_length_freq.sum())
    scaled = session_length_freq.apply(lambda x: x / sum_all_sessions)
    scaled = scaled[scaled > 0.01]
    scaled = scaled.sort_index()
    scaled.plot(kind='bar')
    plt.title("Number of episodes watched in a session")
    plt.show()


def plot_continued_watching(df):
    """
    Produce a bar chart representing the probability for a viewer having seen episode N to continue watching, after
    having seen M episodes.
    """
    df_sorted = df.sort_values(['userId', 'visitStartTime', 'timeWithinVisit'])
    df_sorted['epi_continued'] = False

    row_iterator = df_sorted.iterrows()
    last_i, last = row_iterator.next()
    for i, row in row_iterator:
        if row['userId'] == last['userId'] and row['visitStartTime'] == last['visitStartTime']:
            df_sorted.set_value(last_i, 'epi_continued', True)
        last = row
        last_i = i

    raw_freq = df_sorted['programId'].value_counts()
    continued = df_sorted[df_sorted['epi_continued']]
    continued_count = continued['programId'].value_counts()
    scaled = continued_count.divide(raw_freq)
    scaled.plot(kind='bar')
    plt.title("Proportion who continued watching after n-th episode")
    plt.show()


if __name__ == "__main__":
    views = pd.read_csv(sys.argv[1])
    views = views[views.timeWithinVisit > 0]
    views.programId.replace(episode_map, inplace=True)
    plot_episode_views(views)
    plot_watched_seconds(views)
    plot_watched_episodes_in_session(views)
    plot_continued_watching(views)
