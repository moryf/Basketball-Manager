(ns basket-sim.core
  (:gen-class))

;; Helper function to add the initial box score to a player
(defn with-initial-box-score [player]
  (assoc player
         :stamina 100
         :box-score {:pts 0, :ast 0, :reb 0, :fga 0, :fgm 0, :3pa 0, :3pm 0, :fta 0, :ftm 0, :tov 0}))

;; Nuggets
(def team-a
  {:name "Denver Nuggets"
   :on-court (into {} (map (fn [[pos p]] [pos (with-initial-box-score p)])
   {
     :pg {:name "Jamal Murray"
          :usage-rate 27.5, :shot-dist-2p 0.655, :shot-dist-3p 0.345,
          :fg-perc-2p 0.523, :fg-perc-3p 0.425, :orb-perc 2.1, :drb-perc 12.5,
          :ast-perc-2p 0.45, :ast-perc-3p 0.65, :ast-perc 30.1, :tov-pct 9.9, :ftr 0.226, :ft-perc 0.88}
     :sg {:name "Kentavious Caldwell-Pope"
          :usage-rate 12.8, :shot-dist-2p 0.435, :shot-dist-3p 0.565,
          :fg-perc-2p 0.528, :fg-perc-3p 0.406, :orb-perc 1.9, :drb-perc 7.8,
          :ast-perc-2p 0.70, :ast-perc-3p 0.95, :ast-perc 10.1, :tov-pct 10.3, :ftr 0.129, :ft-perc 0.82}
     :sf {:name "Michael Porter Jr."
          :usage-rate 20.3, :shot-dist-2p 0.463, :shot-dist-3p 0.537,
          :fg-perc-2p 0.596, :fg-perc-3p 0.397, :orb-perc 5.8, :drb-perc 16.6,
          :ast-perc-2p 0.68, :ast-perc-3p 0.90, :ast-perc 6.9, :tov-pct 8.3, :ftr 0.140, :ft-perc 0.79}
     :pf {:name "Aaron Gordon"
          :usage-rate 17.8, :shot-dist-2p 0.887, :shot-dist-3p 0.113,
          :fg-perc-2p 0.612, :fg-perc-3p 0.290, :orb-perc 8.4, :drb-perc 15.0,
          :ast-perc-2p 0.65, :ast-perc-3p 0.98, :ast-perc 14.5, :tov-pct 11.2, :ftr 0.334, :ft-perc 0.72}
     :c  {:name "Nikola Jokic"
          :usage-rate 29.8, :shot-dist-2p 0.826, :shot-dist-3p 0.174,
          :fg-perc-2p 0.643, :fg-perc-3p 0.359, :orb-perc 10.9, :drb-perc 31.8,
          :ast-perc-2p 0.55, :ast-perc-3p 0.85, :ast-perc 42.3, :tov-pct 13.5, :ftr 0.392, :ft-perc 0.78}}))})

;; Warriors
(def team-b
  {:name "Golden State Warriors"
   :on-court (into {} (map (fn [[pos p]] [pos (with-initial-box-score p)])
   {
     :pg {:name "Stephen Curry"
          :usage-rate 29.2, :shot-dist-2p 0.463, :shot-dist-3p 0.537,
          :fg-perc-2p 0.518, :fg-perc-3p 0.408, :orb-perc 2.0, :drb-perc 12.1,
          :ast-perc-2p 0.50, :ast-perc-3p 0.75, :ast-perc 25.4, :tov-pct 12.1, :ftr 0.222, :ft-perc 0.93}
     :sg {:name "Klay Thompson"
          :usage-rate 22.9, :shot-dist-2p 0.383, :shot-dist-3p 0.617,
          :fg-perc-2p 0.493, :fg-perc-3p 0.387, :orb-perc 1.7, :drb-perc 8.7,
          :ast-perc-2p 0.72, :ast-perc-3p 0.96, :ast-perc 13.1, :tov-pct 8.3, :ftr 0.113, :ft-perc 0.91}
     :sf {:name "Andrew Wiggins"
          :usage-rate 18.2, :shot-dist-2p 0.662, :shot-dist-3p 0.338,
          :fg-perc-2p 0.505, :fg-perc-3p 0.358, :orb-perc 5.2, :drb-perc 11.2,
          :ast-perc-2p 0.60, :ast-perc-3p 0.97, :ast-perc 8.4, :tov-pct 9.8, :ftr 0.201, :ft-perc 0.82}
     :pf {:name "Draymond Green"
          :usage-rate 13.5, :shot-dist-2p 0.685, :shot-dist-3p 0.315,
          :fg-perc-2p 0.547, :fg-perc-3p 0.395, :orb-perc 3.7, :drb-perc 21.0,
          :ast-perc-2p 0.65, :ast-perc-3p 0.99, :ast-perc 28.6, :tov-pct 18.2, :ftr 0.244, :ft-perc 0.71}
     :c  {:name "Kevon Looney"
          :usage-rate 10.1, :shot-dist-2p 0.992, :shot-dist-3p 0.008,
          :fg-perc-2p 0.598, :fg-perc-3p 0.000, :orb-perc 14.1, :drb-perc 21.4,
          :ast-perc-2p 0.80, :ast-perc-3p 1.0, :ast-perc 12.5, :tov-pct 13.4, :ftr 0.208, :ft-perc 0.67}}))})

;; Select finisher function based on their usage rate
(defn select-finisher
  "Selects players randomly based on their usage rate"
  [players]
  (let [total-usage (reduce + (map :usage-rate players))]
    (loop [p (rand total-usage)
           [current-player & rest-players] players]
      (if (< p (:usage-rate current-player))
        current-player
        (recur (- p (:usage-rate current-player)) rest-players)))))

;; Function to fetermine a possesion's outcome and pick between turnovers, shots or free-throws
(defn decide-possession-outcome [finisher]
  (let [r (rand)
        tov-pct (/ (:tov-pct finisher) 100)
        ftr (:ftr finisher)]
    (cond
      (< r tov-pct) :turnover
      (< r (+ tov-pct (* (- 1 tov-pct) (/ ftr 2)))) :foul
      :else :shot)))

;; Function that add a penalty to shot percentages based on player's fatigue
(defn apply-fatigue [player shot-percentage]
  (let [stamina (:stamina player)
        fatigue-penalty (cond
                          (> stamina 75) 1.0   
                          (> stamina 50) 0.85  
                          (> stamina 25) 0.65   
                          :else          0.5)] 
    (* shot-percentage fatigue-penalty)))

;;Shot simualtion function
(defn simulate-shot
  "Simulates shot. a Chooses whether the shot is 2p or 3p based on their tendencies. Hit or miss based on shot percentage"
  [player]
  (let [shot-type (if (< (rand) (:shot-dist-3p player)) :3p :2p)
        shot-made? (if (= shot-type :3p)
                     (< (rand) (apply-fatigue player (:fg-perc-3p player)))
                     (< (rand) (apply-fatigue player (:fg-perc-2p player))))]
    {:shot-type shot-type :shot-made? shot-made?}))

;; Rebound simulation function
(defn simulate-rebound
  "Simulates a rebound, selecting a player from all players on court"
  [off-team def-team]
  (let [off-players (vals off-team)
        def-players (vals def-team)
        all-players (concat off-players def-players)
        rebound-chances (map (fn [p] (if (some #{p} off-players) (:orb-perc p) (:drb-perc p))) all-players)
        total-rebound-chance (reduce + rebound-chances)]
    (loop [r (rand total-rebound-chance)
           [player & remaining] all-players
           [chance & chances] rebound-chances]
      (if (< r chance)
        player
        (recur (- r chance) remaining chances)))))

;; Assist simulation function
(defn simulate-assist
  "Simulates a potential assist on a made shot using the hybrid logic."
  [shooter shot-type team]
  (let [assist-chance (if (= shot-type :3p)
                        (:ast-perc-3p shooter)
                        (:ast-perc-2p shooter))]

    (when (< (rand) assist-chance)

      (let [shooter-pos (some (fn [[pos p]] (when (= p shooter) pos)) team)
            potential-assisters (vals (dissoc team shooter-pos))
            total-assist-weight (reduce + (map :ast-perc potential-assisters))]

        (when (pos? total-assist-weight)
          (loop [r (rand total-assist-weight)
                 [player & remaining] potential-assisters]
            (let [weight (:ast-perc player)]
              (if (< r weight)
                player 
                (recur (- r weight) remaining)))))))))

;; Initial game state - fix later, add quarters
(def initial-game-state
  {:game-clock 2880 ; 12 * 60 seconds
   :shot-clock 24
   :offense :team-a 
   :defense :team-b
   :score {:team-a 0 :team-b 0}
   :teams {:team-a team-a :team-b team-b}})

;; Possesion time simulation - fix later, add more complex logic to the game
(defn simulate-possession-time
  "Returns a random amount of time a possession might take."
  [shot-clock]
  (+ 5 (rand-int (- shot-clock 6))))

(defn find-player-pos [team player]
  (some (fn [[pos p]] (when (= p player) pos)) team))

(defn update-stamina [on-court-players]
  (into {} (map (fn [[pos p]]
                  [pos (cond (> (p :stamina) 0) (update p :stamina - 1)
                             :else p)])
                on-court-players)))



(defn resolve-shot
  "Resovles the aftermath of a shot being taken, wether it's a hit, a miss and if there are assists and rebounds"
  [game-state finisher]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        def-team (get-in game-state [:teams def-team-id :on-court])
        finisher-pos (find-player-pos off-team finisher)
        shot-result (simulate-shot finisher)
        possession-time (simulate-possession-time (:shot-clock game-state))
        is-3p? (= :3p (:shot-type shot-result))]
  
  
    (println "-------------------------")
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println (:name finisher) "takes a" (name (:shot-type shot-result)) "pointer...") 
    (if (:shot-made? shot-result)
      (let [assister (simulate-assist finisher (:shot-type shot-result) off-team)
            points (if (= :3p (:shot-type shot-result)) 3 2)
            assister-pos (when assister (find-player-pos off-team assister))]
        (if assister
          (println "It's good! Assist by" (:name assister))
          (println "It's good! Unassisted."))
  
        (-> game-state
            (update :game-clock - possession-time)
            (assoc :shot-clock 24)
            (update-in [:teams :team-a :on-court] update-stamina)
            (update-in [:teams :team-b :on-court] update-stamina)
            (update-in [:score off-team-id] + points)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :pts] + points)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :fgm] inc)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :fga] inc)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pm] (if is-3p? inc identity))
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pa] (if is-3p? inc identity))
            (cond-> assister-pos (update-in [:teams off-team-id :on-court assister-pos :box-score :ast] inc))            (assoc :offense def-team-id)
            (assoc :defense off-team-id)))
  
      (let [rebounder (simulate-rebound off-team def-team)]
        (println "It's a miss!")
        (println "Rebound by:" (:name rebounder))
  
        (let [offensive-rebound? (some #(= rebounder %) (vals off-team))]
          (-> game-state
              (update :game-clock - possession-time)
              (update-in [:teams :team-a :on-court] update-stamina)
              (update-in [:teams :team-b :on-court] update-stamina)
              (update-in [:teams off-team-id :on-court finisher-pos :box-score :fga] inc)
              (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pa] (if is-3p? inc identity))
              (update-in [:teams (if offensive-rebound? off-team-id def-team-id) :on-court (find-player-pos (if offensive-rebound? off-team def-team) rebounder) :box-score :reb] inc)
              (assoc :shot-clock (if offensive-rebound?
                                   14
                                   24))
              (assoc :offense (if offensive-rebound?
                                off-team-id
                                def-team-id))
              (assoc :defense (if offensive-rebound?
                                def-team-id
                                off-team-id))))))))


(defn resolve-turnover
  "Resolves a trunover by updating the box-score for a player and updating the game state and changing the possesion"
  [game-state finisher]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        finisher-pos (find-player-pos off-team finisher)
        possession-time (simulate-possession-time (:shot-clock game-state))
        ] 
    (println "-------------------------")
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println "It's a trunover!")
    (println (finisher :name) " has lost the ball!")
    (-> game-state
        (update :game-clock - possession-time)
        (update-in [:teams :team-a :on-court] update-stamina)
        (update-in [:teams :team-b :on-court] update-stamina)
        (assoc :offense def-team-id)
        (assoc :defense off-team-id)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :tov] inc ))))

(defn resolve-foul
  "Resolves free throws based on players free throw percentage"
  [game-state finisher]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        finisher-pos (find-player-pos off-team finisher)
        possession-time (simulate-possession-time (:shot-clock game-state))
        ft-1 (< (rand) (get-in game-state [:teams off-team-id :on-court finisher-pos :ft-perc]))
        ft-2 (< (rand) (get-in game-state [:teams off-team-id :on-court finisher-pos :ft-perc]))
        made-shots (cond (and ft-1 ft-2) 2
                         (or ft-1 ft-2) 1
                         :else 0)] 
    (println "-------------------------")
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println "It's a foul!")
    (println (finisher :name) " takes free throws")
    (println "He's made " made-shots " of 2 free-throws!")
    (-> game-state
        (update :game-clock - possession-time)
        (update-in [:teams :team-a :on-court] update-stamina)
        (update-in [:teams :team-b :on-court] update-stamina)
        (update-in [:score off-team-id] + made-shots)
        (assoc :offense def-team-id)
        (assoc :defense off-team-id)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :pts] + made-shots)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :ftm] + made-shots)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :fta] + 2))))

;; Run possesion function - simulate a possesion and update game state
(defn run-possession
  "Runs a single possession and returns the new game state."
  [game-state]
  (let [off-team-id (:offense game-state)
        finisher (select-finisher (vals (get-in game-state [:teams off-team-id :on-court])))
        outcome (decide-possession-outcome finisher)] 
    (cond (= outcome :shot) (resolve-shot game-state finisher)
          (= outcome :turnover) (resolve-turnover game-state finisher)
          :else (resolve-foul game-state finisher))))

;; Function to print the box score at the end of the game
(defn print-box-score [game-state]
  (doseq [team-id [:team-a :team-b]]
    (let [team (get-in game-state [:teams team-id])
          team-fga (reduce + (map (comp :fga :box-score) (vals (:on-court team))))
          team-fgm (reduce + (map (comp :fgm :box-score) (vals (:on-court team))))
          team-3pa (reduce + (map (comp :3pa :box-score) (vals (:on-court team))))
          team-3pm (reduce + (map (comp :3pm :box-score) (vals (:on-court team))))
          team-fta (reduce + (map (comp :fta :box-score) (vals (:on-court team))))
          team-ftm (reduce + (map (comp :ftm :box-score) (vals (:on-court team))))
          team-fg-pct (float (* 100 (/ team-fgm team-fga))) 
          team-3p-pct (float (* 100 (/ team-3pm team-3pa)))
          team-ft-pct (float (* 100 (/ team-ftm team-fta)))]
      (println "\n---" (:name team) "---")
      (println "FG : " team-fgm "-" team-fga)
      (println "FG % : " (format "%.2f" team-fg-pct) "%")
      (println "3p : " team-3pm "-" team-3pa)
      (println "3p % :" (format "%.2f "team-3p-pct) "%")
      (println "FT: " team-ftm "-" team-fta)
      (println "FT%: " team-ft-pct)
      (println (format "%-25s %-5s %-5s %-5s %-5s %-10s %-5s %-5s" "PLAYER" "PTS" "REB" "AST" "FG" "3P" "FT" "TO")) 
      
      (doseq [player (vals (:on-court team))]
        (println "Stamina" (player :stamina))
        (let [bs (:box-score player)]
          (println (format "%-25s %-5s %-5s %-5s %s-%-5s %s-%-5s %s-%-5s %-5s"
                           (:name player)
                           (:pts bs)
                           (:reb bs)
                           (:ast bs)
                           (:fgm bs)
                           (:fga bs)
                           (:3pm bs)
                           (:3pa bs)
                           (:ftm bs)
                           (:fta bs)
                           (:tov bs))))))))

;; Function to recur possesions until the time runs out
(defn run-game [initial-state]
  (loop [game-state initial-state]
    (if (<= (:game-clock game-state) 0)
      game-state
      (recur (run-possession game-state)))))

(defn -main
  "Simulates a full game and prints the final box score."
  [& args]
  (println "Simulating game...")
  (let [final-state (run-game initial-game-state)]
    (println "\n--- FINAL SCORE ---")
    (println (:name team-a) ":" (get-in final-state [:score :team-a]))
    (println (:name team-b) ":" (get-in final-state [:score :team-b]))
    (print-box-score final-state)))