#!/usr/bin/env sh

#echo 1 > /proc/sys/net/ipv4/ip_forward
iptables -t nat -A POSTROUTING -s 192.168.11.0/24 -j MASQUERADE
iptables -A FORWARD -o enp5s0 -i wlp4s0 -s 192.168.11.0/24 -m conntrack --ctstate NEW -j ACCEPT
iptables -A FORWARD -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
