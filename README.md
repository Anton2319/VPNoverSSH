# VPN over SSH

## What is it?
VPN over SSH is a systemwide proxy for Android which utilizes [VPN Service API](https://developer.android.com/guide/topics/connectivity/vpn). It allows you to tunnel your traffic through SSH tunnel between your server and your client device. Essentially, this is the alternative of doing a dynamic port forward via openssh cli client: `ssh user@hostname -D 1080`. No additional server setup is needed.

## How does it work?

### Server
Server (which is typically a Linux or Unix machine) runs OpenSSH, which has the [port forwarding](https://github.com/apache/mina-sshd/blob/master/docs/technical/tcpip-forwarding.md) capability built-in.

### Client
The application first establishes a connection with a remote server using [connectbot sshlib](https://github.com/connectbot/sshlib), and sets up a dynamic port forward and a localhost socks5 server on port 1080.

Then, using [tun2socks](https://github.com/xjasonlyu/tun2socks) library as socks5 client, it captures all system traffic and sends it to the localhost proxy server. 


## Limitations

### Lack of UDP support
With no additional server setup, this app cannot tunnel any UDP traffic, which makes it useless for everyday web browsing because services such as YouTube, Twitch, Netflix, and other streaming platforms will not work, as well as many multiplayer games, and other services that rely on UDP to deliver its content.

### Low speed
OpenSSH uses strong encryption, TCP protocol, and typically runs in userspace, all of which dramaticaly slows down your connection. Expect around 10x slowdown. 


 