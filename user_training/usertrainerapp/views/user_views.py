from django.shortcuts import render
from usertrainerapp.serializers import TrainingmoduleSerializer, ReviewSerializer
from rest_framework.decorators import api_view, permission_classes
from usertrainerapp.models import TrainingModule, Review 
from usertrainerapp.permissions import IsUser
from rest_framework.response import Response
from rest_framework import status
from django.contrib import messages

@api_view(['GET',])
@permission_classes([IsUser,])
def render_reviews(request):
    user = request.user
    print(user)
    user_reviews = Review.objects.filter(user = user)
    serializer = ReviewSerializer(user_reviews, many=True)
    print(serializer.data)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(['GET',])
@permission_classes([IsUser,])
def render_modules(request):
    user = request.user
    print(user)
    user_modules = TrainingModule.objects.filter(user=user)
    serializer = TrainingmoduleSerializer(user_modules, many=True)
    print(serializer.data)
    return Response(serializer.data, status=status.HTTP_200_OK)

def show_modules(request):
    return render(request, 'user_templates/modules_list.html')

def show_reviews(request):
    return render(request, 'user_templates/reviews_list.html')

def user_dashboard_view(request):
    return render(request, 'user_templates/user_dashboard.html')