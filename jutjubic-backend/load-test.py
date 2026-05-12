#!/usr/bin/env python3
"""
IMPROVED LOAD TEST ZA JUTJUBIC BACKEND
========================================
Simulira prave korisnike koji šalju zahteve ka aplikaciji.
"""

import requests
import time
import random
import threading
from datetime import datetime
import sys

BASE_URL = "http://localhost:8080"
USERS = 10  # Broj konkurentnih "korisnika"
DURATION = 180  # Trajanje testa u sekundama (3 minuta)

# Disable SSL warnings
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def check_application():
    """Proveri da li je aplikacija pokrenuta"""
    try:
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=5, verify=False)
        return response.status_code == 200
    except Exception as e:
        print(f"Error connecting to backend: {e}")
        return False

def simulate_user(user_id, end_time):
    """Simulira jednog korisnika koji šalje zahteve"""
    actions_count = 0
    session = requests.Session()
    session.verify = False

    print(f"🚀 User {user_id} started")

    while time.time() < end_time:
        action = random.randint(0, 5)

        try:
            if action == 0:
                # Health check - GARANTOVANO radi
                response = session.get(f"{BASE_URL}/actuator/health", timeout=5)
            elif action == 1:
                # Prometheus metrics - GARANTOVANO radi
                response = session.get(f"{BASE_URL}/actuator/prometheus", timeout=5)
            elif action == 2:
                # Test endpoint (ako postoji)
                response = session.get(f"{BASE_URL}/api/test", timeout=5)
            elif action == 3:
                # Public posts (ako postoji)
                response = session.get(f"{BASE_URL}/api/posts/public", timeout=5)
            elif action == 4:
                # Login attempt (POST request)
                response = session.post(
                    f"{BASE_URL}/api/auth/login",
                    json={"email": f"user{user_id}@test.com", "password": "test123"},
                    timeout=5
                )
            else:
                # Metrics endpoint
                response = session.get(f"{BASE_URL}/actuator/metrics", timeout=5)

            actions_count += 1

            if actions_count % 50 == 0:
                print(f"📊 User {user_id}: {actions_count} requests sent")

        except Exception as e:
            # Ignoriši greške, nastavi sa testom
            pass

        # Random pauza između 0.05 i 0.3 sekundi (više zahteva!)
        time.sleep(random.uniform(0.05, 0.3))

    print(f"✅ User {user_id} finished ({actions_count} requests)")

def get_metric(metric_name):
    """Uzmi vrednost metrike iz Prometheus endpoint-a"""
    try:
        response = requests.get(f"{BASE_URL}/actuator/prometheus", timeout=5, verify=False)
        for line in response.text.split('\n'):
            if metric_name in line and not line.startswith('#'):
                parts = line.split()
                if len(parts) >= 2:
                    return parts[-1]
        return "N/A"
    except:
        return "N/A"

def monitor_progress(start_time, duration):
    """Prati progres testa"""
    while time.time() - start_time < duration:
        elapsed = int(time.time() - start_time)
        remaining = duration - elapsed

        # Uzmi metrike
        active_conn = get_metric('hikaricp_connections_active{pool="HikariPool-1"}')

        try:
            cpu_usage_str = get_metric('system_cpu_usage')
            cpu_usage = float(cpu_usage_str) * 100
            cpu_str = f"{cpu_usage:.1f}%"
        except:
            cpu_str = "N/A"

        try:
            req_rate = get_metric('http_server_requests_seconds_count')
            req_str = f"{req_rate}"
        except:
            req_str = "N/A"

        current_time = datetime.now().strftime("%H:%M:%S")
        print(f"\n[{current_time}] ⏱️  {remaining}s remaining | 🔌 DB: {active_conn} | 💻 CPU: {cpu_str} | 📈 Requests: {req_str}")

        time.sleep(5)

def main():
    print("=" * 60)
    print("🚀 IMPROVED LOAD TEST - Jutjubic Backend")
    print("=" * 60)
    print(f"Target: {BASE_URL}")
    print(f"Duration: {DURATION} seconds ({DURATION//60} minutes)")
    print(f"Concurrent users: {USERS}")
    print("=" * 60)
    print()

    # Proveri da li je aplikacija pokrenuta
    print("Checking if application is running...")
    if not check_application():
        print("❌ ERROR: Application is not running!")
        print("Please start the application first!")
        sys.exit(1)

    print("✅ Application is running!")
    print()

    # Pokreni test
    print(f"Starting {USERS} concurrent users...")
    print(f"Test will run for {DURATION} seconds")
    print()

    start_time = time.time()
    end_time = start_time + DURATION

    # Pokreni korisnike u thread-ovima
    threads = []
    for i in range(1, USERS + 1):
        thread = threading.Thread(target=simulate_user, args=(i, end_time))
        thread.start()
        threads.append(thread)
        time.sleep(0.1)  # Malo odstojanje između startova

    # Pokreni monitoring
    monitor_thread = threading.Thread(target=monitor_progress, args=(start_time, DURATION))
    monitor_thread.start()

    # Sačekaj sve thread-ove
    for thread in threads:
        thread.join()

    monitor_thread.join()

    print()
    print("=" * 60)
    print("✅ Load test completed!")
    print("=" * 60)
    print()
    print("📊 Check your metrics at:")
    print("   Prometheus: http://localhost:9090")
    print("   Grafana: http://localhost:3000")
    print()
    print("🔍 Useful Prometheus queries:")
    print("   - rate(http_server_requests_seconds_count[1m])")
    print("   - hikaricp_connections_active")
    print("   - system_cpu_usage * 100")
    print()

if __name__ == "__main__":
    main()