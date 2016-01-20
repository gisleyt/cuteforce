#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys
import json

'''
Read input data sorted by user id, and print session events, which contains information about the entire
viewing session for a user.

Usage: $ sort -n events.csv | python sessions.py | python analyze.py
'''


class Session:
    def __init__(self, id, start):
        self.id = id
        self.start = start
        self.duration = 0
        self.episodes = []


if __name__ == "__main__":
    current_user = Session('', 0)
    for line in sys.stdin:
        if line.startswith("userId"):
            continue
        line = line.strip().split(',')
        if not (line[0] == current_user.id and int(line[2]) == current_user.start):
            # Skip header line, and events where timeWithinVisit is 0.
            if current_user.id != '' and current_user.duration > 0:
                # Print json of Session object.
                print json.dumps(current_user.__dict__)
            current_user = Session(line[0], int(line[2]))

        duration = int(line[3])
        if duration > 0:
            current_user.episodes.append('ep' + line[1][-3])
            current_user.duration += duration

    # Print final session
    print json.dumps(current_user.__dict__)
