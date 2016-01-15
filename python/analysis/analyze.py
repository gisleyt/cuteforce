#!/usr/bin/env python
# -*- coding: utf-8 -*-
import pandas as pd
from bisect import bisect
from matplotlib.pylab import *
import json

'''
Analyze event logs and produce stats.

Usage: $ sort -n events.csv | python sessions.py | python analyze.py
'''


def update_watched_episodes(episodes, session, max_length):
    """
    Update the map of episodes with the number of seconds spent on watching an episode.
    We *only* consider sessions with one single episode view.
    """
    if len(session['episodes']) == 1:
        singleton_episode_watched = session['episodes'][0]
        if singleton_episode_watched not in episodes:
            episodes[singleton_episode_watched] = []
        list_of_episode_durations = episodes[singleton_episode_watched]
        list_of_episode_durations.append(min(session['duration'] / 1000, max_length))


def update_viewers_per_episode(episode_map, sessions):
    """
    The simplest, less error-prone, and therefore (imho) arguably the BEST way to retrieve the number of
    users who has watched each episode, would simply be to use some unix magic.

    $ grep "KMTE20000114" events.csv  | grep -v ",0$" | sort | cut -d',' -f 1 | uniq | wc -l
    106418
    $ grep "KMTE20000214" events.csv  | grep -v ",0$" | sort | cut -d',' -f 1 | uniq | wc -l
    90504
    
    The good thing with doing it this way, is that I can sanity-check the pre-processing stage, by verifying that
    the numbers check out.
    """
    watched_episodes = []
    for session in sessions:
        watched_episodes.extend(session['episodes'])
    watched_episodes = list(set(watched_episodes))
    for episode in watched_episodes:
        if episode not in episode_map:
            episode_map[episode] = 0
        episode_map[episode] += 1


def update_session_length(session, session_map):
    """
    Increment map containing the frequency for number of episodes contained in this session.
    """
    session_length = len(session['episodes'])
    if session_length not in session_map:
        session_map[session_length] = 0
    session_map[session_length] += 1


def update_continuous_watching(session, all_instances, continued_to_watch):
    """
    Update matrices for users who have finished watching episode N as the M-th episode in the session.
    """
    for i in range(len(session['episodes'])):
        if i >= len(all_instances.index):
            break

        column = session['episodes'][i]
        if column not in all_instances:
            all_instances[column] = 0
            continued_to_watch[column] = 0

        # The matrix index starts at 1, to reflect that the row represents users who have seen 1 episode.
        episodes_watched_in_session = i + 1

        # Increment.
        all_instances.set_value(episodes_watched_in_session, column,
                                all_instances.get_value(episodes_watched_in_session, column) + 1)
        if i != len(session['episodes']) - 1:
            # If the current episode is not the last episode in the session, we increment this matrix value for the matrix containing
            # the frequencies for users who continue watching after having completed episode N as their M-the episode in the session.
            continued_to_watch.set_value(episodes_watched_in_session, column,
                                         continued_to_watch.get_value(episodes_watched_in_session, column) + 1)


def plot_viewers(viewers):
    """
    Plot the number of viewers per episode.
    """
    viewer_series = pd.Series(viewers, name='Viewers')
    fig, ax = plt.subplots(1, 1)
    ax.get_xaxis().set_visible(False)
    viewer_series.plot(table=True, kind='bar')
    title("Viewers per episode")
    savefig("/tmp/viewer_per_episode.png")
    close()


def plot_continued(all_instances, continued_watching):
    """
    Produce a bar chart representing the probability for a viewer having seen episode N to continue watching, after
    having seen M episodes.
    """

    proportions = continued_watching.div(all_instances).fillna(0)

    # Save the matrix containing all probabilities.
    proportions.to_csv("/tmp/proportions.txt")
    episode_map = {}

    for episode in all_instances.columns:
        episode_map[episode] = float(continued_watching[episode].sum()) / all_instances[episode].sum()
    episode_series = pd.Series(episode_map)
    episode_series.plot(kind='bar')
    title("Proportion who continued watching after n-th episode")
    savefig("/tmp/continued.png")
    close()


def plot_session_lengths(lengths):
    """
    Plot the proportion of the sessions that consists of one episode view, two episode views, etc.
    """
    sum = pd.Series(lengths.values()).sum()
    for length in lengths:
        freq = float(lengths[length])
        # Compute the proportion, rather than the frequency.
        lengths[length] = freq / sum

    length_series = pd.Series(lengths)

    # Prune away session lengths with low frequency.
    length_series = length_series[length_series > 0.01]

    length_series.plot(kind='bar')
    title("Number of episodes watched in a session")
    savefig("/tmp/session_length.png")
    close()


def plot_watched_episode(episode_durations, max_length):
    """
    Plot a graph where the x-axis represents seconds watched in an episode, and the
    y-axis represents the proportion of viewers who watched (at least) x many seconds.
    """
    episode_durations = np.sort(episode_durations)
    x = np.linspace(1, episode_durations[-1], num=100)
    y = []
    for seconds in x:
        insertion = float(bisect(episode_durations, seconds))
        y.append((len(episode_durations) - insertion) / len(episode_durations))
    title("Unge Lovende")
    xlabel('Seconds watched')
    ylabel('Proportion of population')
    axis([0, max_length, 0.6, 1.0])
    plot(x, y)


def plot_viewers_per_seconds():
    """
    Visualize the proportion of users who watched the entire episode.
    """
    episodes_sorted = seconds_watched.keys()
    episodes_sorted.sort()
    for episode in episodes_sorted:
        plot_watched_episode(seconds_watched[episode], 1800)
    legend(episodes_sorted)
    savefig("/tmp/viewer_seconds_of_episode.png")
    close()


if __name__ == "__main__":

    # Episode-maps to a list of seconds. Each element in the seconds list represents
    # how much time the user spent watching the episode.
    # Ex. "ep1": [452, 1400, 809,..], "ep2":[32, 335, 1049,...]
    seconds_watched = {}

    # Episode map with counter for how many viewers each episode had.
    # Ex. "ep1": 4493, "ep2": 9742, etc
    viewers_per_episode = {}

    # List of all sessions for an individual user. To be used when updating the viewers_per_episode map
    sessons_for_user = []

    # Map the number of episodes within a session to its frequency.
    # Ex. "sessions with 1 episode" : 312 events, "sessions with two episodes": 132 events", etc.
    sessions_lengths = {}

    # Matrix where each episode is a column. The rows represent the total number of episodes watched within the
    # session, and we hard-code this so that we only consider users who have watched up to (and including) the fifth
    # episode. An index in the matrix will be incremented when a user has finished episode N, as the M-th episode
    # in the session.
    # Example:
    #       ep1    ep2    ep3    ep4    ep5    ep6
    # 1  110901  45733  39356  35947  38447  30468
    # 2     685  48357  21411  19939  17508  21739
    # 3      21    556  28262  12843  10855  11661
    # 4       2     91    444  18756   7923   7822
    # 5       0      4     97    378  12503   6046
    all_users_and_episodes = pd.DataFrame(index=range(1, 6))

    # Similar to the matrix above, but an index is only incremented when the user continues to watch a new episode
    # within the session.
    #
    #     ep1    ep2    ep3    ep4    ep5  ep6
    # 1  49543  21269  19838  17247  21469  273
    # 2    335  28637  12834  10761  11512  119
    # 3     10    374  18967   7868   7744   75
    # 4      2     70    342  12603   5970   41
    # 5      0      3     81    264   9864   46
    continued_to_watch = pd.DataFrame(index=range(1, 6))

    last_id = ''

    for line in sys.stdin:
        session = json.loads(line.strip())
        # Hard-code max-length of an episode to 30 minutes.
        update_watched_episodes(seconds_watched, session, 1800)

        # Update the length, i.e. number of watched episodes in session.
        update_session_length(session, sessions_lengths)

        # Update number of (unique) viewers per episode.
        if session['id'] != last_id and len(sessons_for_user) > 0:
            update_viewers_per_episode(viewers_per_episode, sessons_for_user)
            sessons_for_user = []
        sessons_for_user.append(session)
        last_id = session['id']

        # Update matrices of session lengths related to the currently watched episode.
        update_continuous_watching(session, all_users_and_episodes, continued_to_watch)

    # Update data for the final user before the for-loop terminated.
    update_viewers_per_episode(viewers_per_episode, sessons_for_user)

    # Visualize probability fir continuing the session.
    plot_continued(all_users_and_episodes, continued_to_watch)

    # Distribution of session lengths.
    plot_session_lengths(sessions_lengths)

    # Number of viewers/episode.
    plot_viewers(viewers_per_episode)

    # Visualize how far into an episode a viewer watched.
    plot_viewers_per_seconds()
