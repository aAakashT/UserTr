from django.shortcuts import render, redirect
from usertrainerapp.serializers import TrainingmoduleSerializer, UserSerializer
from usertrainerapp.models import TrainingModule, User 
from usertrainerapp.permissions import IsAdmin
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth.models import Group
from rest_framework.response import Response
from django.db.models import Q

class TrainingCreateView(APIView):
    permission_classes = [IsAdmin]
    def post(self, request):
        serializer = TrainingmoduleSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class UserListView(APIView):
    permission_classes = [IsAdmin]
    def get(self, request):
        try:
            users = User.objects.filter(~Q(groups=1), is_active=True).prefetch_related('modules', 'reviews').exclude()
        except User.DoesNotExist:
            return Response({'error': "users does not exists"})
        serializer = UserSerializer(users, many=True)
        print(serializer.data)
        return Response({'data':serializer.data})

def show_users(request):
    return render(request, 'admin_templates/user_list.html')

class TrainingListView(APIView):
    permission_classes = [IsAdmin]
    def get(self, request):
        training_modules = TrainingModule.objects.all()
        serializer = TrainingmoduleSerializer(training_modules, many=True)
       
        return Response(serializer.data)

def show_training_modules(request):
    return render(request, 'admin_templates/module_list.html')

class AssignTLView(APIView):
    permission_classes = [IsAdmin]

    def post(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except:
            return Response({'error': 'TL ID is required.'}, status=400)
        tl_group, created = Group.objects.get_or_create(name='Team_Leader')
        if user.team_leader:
            return Response({'error': 'can only assign Tl to user group'}, status=400)

        tl_id = request.data.get('tl_id')

        if not tl_id:
            return Response({'error': 'TL ID is required.'}, status=400)
        try:
            tl = User.objects.get(id=tl_id)
        except User.DoesNotExist:
            return redirect('user_list')  
        user.team_leader = tl
        user.save()
        return Response({'success': 'TL assigned successfully.'})

class AssignRoleView(APIView):
    permission_classes = [IsAdmin]

    def post(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User is required.'}, status=400)
        tl_group, created = Group.objects.get_or_create(name='Team_Leader')
        if not tl_group:
            return Response({'error': 'Role is required.'}, status=400)
        if user.team_leader_id:
            return Response({'error': 'Can not assign tl role to user otherthan user'})
        user.groups.add(tl_group)
        return Response({'success': 'Role assigned successfully.'})
