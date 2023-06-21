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
from rest_framework.renderers import TemplateHTMLRenderer

class TrainingCreateView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/training_create.html'

    def get(self, request):
        try:
            Training = TrainingModule.objects.get(id=1)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        return Response({'Training': Training})

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
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/assign_tl.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)

        context = {'user': user}
        return Response(context)

    def post(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except:
            return Response({'error': 'TL ID is required.'}, status=400)
        # tl_group, created = Group.objects.get_or_create(name='Team_Leader')
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
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/assign_role.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        return Response({'user': user})

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

class UserDeleteView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/delete_user.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)

        context = {'user': user}
        return Response(context)

    def put(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        user.is_active = False
        user.save()
        return Response({'success': 'User deleted successfully.'})

class UserUpdateView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/update_user.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        try:
            team_leaders = User.objects.filter(~Q(id=user_id),~Q(groups__in=[1, 3]), is_active=True)
        except User.DoesNotExist:
            return Response({'error': 'TeamLeaders not found.'}, status=404)

        context = {'user': user, 'team_leaders': team_leaders}
        return Response(context)
    
    def put(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        serializer = UserSerializer(user, data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class TrainingUpdateView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/training_update.html'
    
    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)

        context = {'user': user}
        return Response(context)

    def put(self, request, module_id):
        try:
            training_module = TrainingModule.objects.get(id=module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'Training module does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        serializer = TrainingmoduleSerializer(training_module, data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class TrainingDeleteView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/training_delete.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)

        context = {'user': user}
        return Response(context)

    def delete(self, request, module_id):
        try:
            training_module = TrainingModule.objects.get(id=module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'Training module does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        training_module.delete()
        return Response({'success': 'Training module deleted successfully.'})
