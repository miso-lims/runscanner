---
title: Home Page
layout: default
order: 0
---

Run Scanner is a web service which monitors output directories for sequencing instruments and serves metadata for runs. 

{% for cat in site.category-list %}
### {{ cat }}
<ol>
	{% assign sorted_pages = site.pages | sort: "order" %}
	{% for page in sorted_pages %}
		{% for pc in page.categories %}
			{% if pc == cat %}
				<li><a href="{{ site.baseurl }}{{ page.url }}">{{ page.title }}</a></li>
			{% endif %} <!-- pc == cat -->
		{% endfor %} <!-- page.categories -->
	{% endfor %} <!-- sorted_pages -->
</ol>
{% endfor %} <!-- site.category-list -->
