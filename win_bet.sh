#!/bin/bash

# This script places bets in parallel until a user wins the jackpot.

# Display menu for jackpot selection
echo "Select jackpot type:"
echo "1) jackpot-fixed-fixed"
echo "2) jackpot-fixed-variable"
echo "3) jackpot-variable-fixed"
echo "4) jackpot-variable-variable"
read -p "Enter selection (1-4): " JACKPOT_SELECTION

case $JACKPOT_SELECTION in
  1)
    JACKPOT_ID="jackpot-fixed-fixed"
    ;;
  2)
    JACKPOT_ID="jackpot-fixed-variable"
    ;;
  3)
    JACKPOT_ID="jackpot-variable-fixed"
    ;;
  4)
    JACKPOT_ID="jackpot-variable-variable"
    ;;
  *)
    echo "Invalid selection. Exiting."
    exit 1
    ;;
esac

echo "Selected jackpot: $JACKPOT_ID"

# 1. Authenticate and get JWT token
echo "Authenticating and getting JWT token..."
JWT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1",
    "password": "password123"
  }' | jq -r '.token')

if [ -z "$JWT_TOKEN" ]; then
  echo "Failed to get JWT token. Exiting."
  exit 1
fi

echo "Token: $JWT_TOKEN"

# This file will be created when a bet wins, signaling other processes to stop.
WIN_FLAG="/tmp/jackpot_won_$$"

# Cleanup function to remove the flag file and kill all child processes of this script.
cleanup() {
    echo "Cleaning up..."
    rm -f "$WIN_FLAG"
    # pkill -P $$ will kill all child processes of this script.
    pkill -P $$
}

# Set the trap to call cleanup on script exit.
trap cleanup EXIT

# Function to continuously place bets until a win is detected.
place_bets_worker() {
  while [ ! -f "$WIN_FLAG" ]; do
    bet_id="bet-$(date +%s)-$RANDOM"

    # Submit a bet
    # echo "Submitting bet (ID: $bet_id)..." # This is too noisy in parallel
    curl -s -X POST http://localhost:8080/api/bets \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -d "{
        \"betId\": \"$bet_id\",
        \"jackpotId\": \"$JACKPOT_ID\",
        \"betAmount\": 60000.50
      }"

    # If another process has won while we were betting, exit.
    if [ -f "$WIN_FLAG" ]; then
      break
    fi

    # Evaluate the reward
    response=$(curl -s -X POST "http://localhost:8080/api/jackpots/$JACKPOT_ID/evaluate-reward?betId=$bet_id&userId=1" \
      -H "Authorization: Bearer $JWT_TOKEN")

    # Check if the bet won
    if [ "$(echo "$response" | jq -r '.won')" == "true" ]; then
      # Check if we are the first to win to avoid race conditions
      if [ ! -f "$WIN_FLAG" ]; then
        reward_amount=$(echo "$response" | jq -r '.rewardAmount')
        echo "Congratulations! Bet (ID: $bet_id) won a reward of $reward_amount!"
        # Create the flag file to signal the win
        touch "$WIN_FLAG"
      fi
    fi
  done
}

## 2. Start 20 workers to place bets in parallel
#echo "Starting 20 parallel workers to place bets... This will run until a jackpot is won."
#for i in $(seq 1 20); do
#  place_bets_worker &
#done

place_bets_worker &

# Wait for any of the background jobs to finish.
# Since they only finish on a win, this effectively waits for the first win.
wait -n

echo "Jackpot won! Main script is terminating."