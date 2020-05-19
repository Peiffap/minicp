import networkx as nx
import matplotlib.pyplot as plt

G = nx.DiGraph()

with open('out', 'r') as f:
    lines = f.readlines()
    n_vehicles = int(lines[0].strip())
    d = lines[1].strip().split()
    G.add_node('D', pos=(float(d[1]), float(d[2])))
    i = 2
    labels = {}
    labels['D'] = 'D'
    while 'pickup' in lines[i]:
        p = lines[i].strip().split()
        G.add_node('p'+p[1], pos=(float(p[2]), float(p[3])))
        labels['p'+p[1]] = p[1]
        i+=1
    k = i-2
    while 'drop' in lines[i]:
        p = lines[i].strip().split()
        G.add_node('d'+p[1], pos=(float(p[2]), float(p[3])))
        labels['d' + p[1]] = p[1]
        i+=1
    vehicles = []
    for line in lines[i:]:
        e = line.strip().split()
        from_n = int(e[1])
        if from_n < n_vehicles:
            f = 'D'
        elif from_n < n_vehicles + k:
            f = 'p'+str(from_n - n_vehicles)
        else:
            f = 'd'+str(from_n - n_vehicles - k)
        to_n = int(e[2])
        if to_n < n_vehicles:
            t = 'D'
        elif to_n < n_vehicles + k:
            t = 'p' + str(to_n - n_vehicles)
        else:
            t = 'd' + str(to_n - n_vehicles - k)
        G.add_edge(f, t)
        if int(e[0]) == len(vehicles) - 1:
            vehicles[int(e[0])].append((f, t))
        else:
            vehicles.append([(f, t)])

pos=nx.get_node_attributes(G,'pos')
colors = [0] + [0.5 for _ in range(k)] + [1.0 for _ in range(k)]
nx.draw_networkx_nodes(G,pos, with_labels=True, node_color=colors, font_color='white', node_size=200)

colors = ['r', 'g', 'b', 'y', 'p']
for i in range(len(vehicles)):
    nx.draw_networkx_edges(G, pos, edgelist=vehicles[i], edge_color=colors[i])

nx.draw_networkx_labels(G, pos, labels, font_size=10, font_color='w')

plt.show()